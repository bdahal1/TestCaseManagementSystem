package com.tcms.automation;

import com.microsoft.playwright.*;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.AriaRole;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutomationBrowserDriver implements AutoCloseable {
    private final Map<String, AutomationTestDefinition.LocatorDefinition> locators;
    private final Map<String, String> variables;
    private final Set<String> secretValues;
    private final double stepTimeoutMs;
    private final StringBuilder diagnosticLogs = new StringBuilder();

    private final java.util.function.BooleanSupplier cancelled;

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    public AutomationBrowserDriver(AutomationTestDefinition definition) {
        this(definition, () -> false);
    }

    public AutomationBrowserDriver(AutomationTestDefinition definition, java.util.function.BooleanSupplier cancelled) {
        this.cancelled = cancelled;
        this.locators = definition.getLocators() != null ? definition.getLocators() : Collections.emptyMap();
        this.variables = definition.getVariables() != null ? definition.getVariables() : Collections.emptyMap();
        this.secretValues = new HashSet<>();

        if (definition.getSecretKeys() != null) {
            for (String key : definition.getSecretKeys()) {
                String val = variables.get(key);
                if (val != null && !val.isBlank()) {
                    secretValues.add(val);
                }
                String envVal = System.getenv(key);
                if (envVal != null && !envVal.isBlank()) {
                    secretValues.add(envVal);
                }
            }
        }

        double timeout = 5000.0;
        if (definition.getExecutionConfig() != null && definition.getExecutionConfig().containsKey("stepTimeoutMs")) {
            try {
                timeout = Double.parseDouble(definition.getExecutionConfig().get("stepTimeoutMs"));
            } catch (NumberFormatException ignored) {}
        }
        this.stepTimeoutMs = timeout;
    }

    public void init(boolean headless) {
        log("Initializing Playwright browser (headless=" + headless + ")");
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless));
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 720));
        page = context.newPage();
        page.setDefaultTimeout(stepTimeoutMs);
        page.setDefaultNavigationTimeout(stepTimeoutMs);
        log("Browser context and page initialized with step timeout: " + (int) stepTimeoutMs + "ms");
    }

    public Page getPage() {
        return page;
    }

    public void log(String message) {
        String redacted = redact(message);
        String timestamp = java.time.Instant.now().toString();
        diagnosticLogs.append("[").append(timestamp).append("] ").append(redacted).append("\n");
    }

    public String getDiagnosticLogs() {
        return diagnosticLogs.toString();
    }

    public String redact(String text) {
        if (text == null) return null;
        String result = text;
        for (String secret : secretValues) {
            if (secret != null && !secret.isBlank()) {
                result = result.replace(secret, "[REDACTED]");
            }
        }
        return result;
    }

    public String resolveVariables(String input) {
        if (input == null) return null;
        Pattern pattern = Pattern.compile("\\$\\{([a-zA-Z0-9_.-]+)\\}");
        Matcher matcher = pattern.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String varName = matcher.group(1);
            String replacement = variables.get(varName);
            if (replacement == null) {
                replacement = System.getenv(varName);
            }
            if (replacement == null) {
                replacement = matcher.group(0); // keep original if unresolved
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public Locator resolveLocator(String rawLocator) {
        requireActive();
        if (rawLocator == null || rawLocator.isBlank()) {
            throw new IllegalArgumentException("Target locator is empty");
        }
        String resolved = resolveVariables(rawLocator).trim();
        LocatorType type = null;

        // Check project named locators
        if (locators.containsKey(resolved)) {
            var def = locators.get(resolved);
            type = def.getType();
            resolved = resolveVariables(def.getValue());
            log("Resolved named locator '" + rawLocator + "' to " + type + "='" + resolved + "'");
        }

        if (type == null) {
            if (resolved.startsWith("css=")) {
                type = LocatorType.CSS;
                resolved = resolved.substring(4);
            } else if (resolved.startsWith("xpath=")) {
                type = LocatorType.XPATH;
                resolved = resolved.substring(6);
            } else if (resolved.startsWith("id=")) {
                type = LocatorType.ID;
                resolved = resolved.substring(3);
            } else if (resolved.startsWith("testid=")) {
                type = LocatorType.TEST_ID;
                resolved = resolved.substring(7);
            } else if (resolved.startsWith("role=")) {
                type = LocatorType.ROLE_NAME;
                resolved = resolved.substring(5);
            } else if (resolved.startsWith("//") || resolved.startsWith("./") || resolved.startsWith("(")) {
                type = LocatorType.XPATH;
            } else {
                type = LocatorType.CSS;
            }
        }

        try {
            return switch (type) {
                case ID -> page.locator("#" + resolved);
                case CSS -> page.locator(resolved);
                case XPATH -> page.locator("xpath=" + resolved);
                case TEST_ID -> page.getByTestId(resolved);
                case ROLE_NAME -> resolveRoleLocator(resolved);
                case NAMED -> page.locator(resolved);
            };
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid locator definition: " + rawLocator + " (" + e.getMessage() + ")", e);
        }
    }

    private Locator resolveRoleLocator(String roleExpr) {
        // e.g. button[name='Sign In'] or button
        Pattern rolePattern = Pattern.compile("([A-Za-z]+)(?:\\[name=['\"]([^'\"]+)['\"]\\])?");
        Matcher m = rolePattern.matcher(roleExpr);
        if (m.matches()) {
            String roleName = m.group(1).toUpperCase();
            String name = m.group(2);
            AriaRole ariaRole = AriaRole.valueOf(roleName);
            if (name != null && !name.isBlank()) {
                return page.getByRole(ariaRole, new Page.GetByRoleOptions().setName(name));
            }
            return page.getByRole(ariaRole);
        }
        throw new IllegalArgumentException("Invalid role locator syntax: " + roleExpr + ". Expected format: role[name='accessibleName']");
    }

    // Step Operations
    private void requireActive() {
        if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted())
            throw new IllegalStateException("Automation job lease lost or execution cancelled");
    }

    public void openPage(String rawUrl) {
        requireActive();
        String url = resolveVariables(rawUrl);
        log("Navigating to URL: " + url);
        page.navigate(url);
        page.waitForLoadState();
    }

    public void enterText(String rawLocator, String rawValue) {
        String value = resolveVariables(rawValue);
        log("Entering text into locator: " + rawLocator + " (value: " + value + ")");
        try {
            Locator locator = resolveLocator(rawLocator);
            locator.fill(value);
        } catch (PlaywrightException e) {
            handlePlaywrightException(rawLocator, e);
        }
    }

    public void clickElement(String rawLocator) {
        log("Clicking element: " + rawLocator);
        try {
            Locator locator = resolveLocator(rawLocator);
            locator.click();
        } catch (PlaywrightException e) {
            handlePlaywrightException(rawLocator, e);
        }
    }

    public void verifyTextEquals(String rawLocator, String rawExpected) {
        String expected = resolveVariables(rawExpected);
        log("Verifying text equals on " + rawLocator + " expected: '" + expected + "'");
        try {
            Locator locator = resolveLocator(rawLocator);
            PlaywrightAssertions.assertThat(locator).hasText(expected, new LocatorAssertions.HasTextOptions().setTimeout(stepTimeoutMs));
        } catch (PlaywrightException e) {
            handlePlaywrightException(rawLocator, e);
        }
    }

    public void verifyTextContains(String rawLocator, String rawExpected) {
        String expected = resolveVariables(rawExpected);
        log("Verifying text contains on " + rawLocator + " expected: '" + expected + "'");
        try {
            Locator locator = resolveLocator(rawLocator);
            PlaywrightAssertions.assertThat(locator).containsText(expected, new LocatorAssertions.ContainsTextOptions().setTimeout(stepTimeoutMs));
        } catch (PlaywrightException e) {
            handlePlaywrightException(rawLocator, e);
        }
    }

    public void verifyElementState(String rawLocator, ElementState state) {
        log("Verifying element state on " + rawLocator + " is " + state);
        try {
            Locator locator = resolveLocator(rawLocator);
            if (state == ElementState.VISIBLE) {
                PlaywrightAssertions.assertThat(locator).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(stepTimeoutMs));
            } else {
                PlaywrightAssertions.assertThat(locator).isHidden(new LocatorAssertions.IsHiddenOptions().setTimeout(stepTimeoutMs));
            }
        } catch (PlaywrightException e) {
            handlePlaywrightException(rawLocator, e);
        }
    }

    public byte[] takeScreenshot() {
        if (page != null) {
            try {
                return page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
            } catch (Exception e) {
                log("Failed to capture screenshot: " + e.getMessage());
            }
        }
        return null;
    }

    private void handlePlaywrightException(String rawLocator, PlaywrightException e) {
        String msg = e.getMessage() != null ? e.getMessage() : "";
        if (msg.contains("strict mode violation")) {
            throw new AssertionError("Ambiguous target locator '" + rawLocator + "': matches multiple elements on the page. Please refine locator.");
        }
        if (msg.contains("Timeout") && msg.contains("waiting for locator")) {
            throw new AssertionError("Step failed: element '" + rawLocator + "' was not found within timeout (" + (int) stepTimeoutMs + "ms).");
        }
        throw e;
    }

    @Override
    public void close() {
        log("Cleaning up browser context and resources");
        try {
            if (context != null) context.close();
        } catch (Exception ignored) {}
        try {
            if (browser != null) browser.close();
        } catch (Exception ignored) {}
        try {
            if (playwright != null) playwright.close();
        } catch (Exception ignored) {}
    }
}
