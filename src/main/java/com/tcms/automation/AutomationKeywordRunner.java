package com.tcms.automation;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class AutomationKeywordRunner {
    private final AutomationStepSerializer serializer;

    public AutomationKeywordRunner(AutomationStepSerializer serializer) {
        this.serializer = serializer;
    }

    public AutomationExecutionResult execute(AutomationTestDefinition definition) {
        return execute(definition, () -> false);
    }

    public AutomationExecutionResult execute(AutomationTestDefinition definition, java.util.function.BooleanSupplier cancelled) {
        long startTime = System.currentTimeMillis();
        List<AutomationExecutionResult.StepResult> stepResults = new ArrayList<>();
        byte[] failureScreenshot = null;
        String errorMessage = null;
        JobStatus finalStatus = JobStatus.COMPLETED;

        AutomationBrowserDriver driver = null;
        try {
            driver = new AutomationBrowserDriver(definition, cancelled);
            boolean headless = true;
            if (definition.getExecutionConfig() != null && "false".equalsIgnoreCase(definition.getExecutionConfig().get("headless"))) {
                headless = false;
            }
            try {
                driver.init(headless);
            } catch (Exception e) {
                return AutomationExecutionResult.builder()
                        .status(JobStatus.INFRASTRUCTURE_ERROR)
                        .durationMs(System.currentTimeMillis() - startTime)
                        .errorMessage("Infrastructure error launching browser: " + e.getMessage())
                        .diagnosticLogs("Failed to initialize browser: " + e.getMessage())
                        .build();
            }

            for (AutomationStepModel step : definition.getSteps()) {
                long stepStart = System.currentTimeMillis();
                String desc = serializer.toKeywordDescription(step);
                String keyword = step.getKeyword() != null ? step.getKeyword() : "";
                int order = step.getOrder();

                try {
                    executeStep(driver, step);
                    long stepDuration = System.currentTimeMillis() - stepStart;
                    stepResults.add(AutomationExecutionResult.StepResult.builder()
                            .stepOrder(order)
                            .keyword(keyword)
                            .description(desc)
                            .passed(true)
                            .durationMs(stepDuration)
                            .build());
                } catch (AssertionError | Exception e) {
                    long stepDuration = System.currentTimeMillis() - stepStart;
                    failureScreenshot = driver.takeScreenshot();
                    String error = driver.redact(e.getMessage());
                    errorMessage = "Step " + order + " failed: " + error;

                    stepResults.add(AutomationExecutionResult.StepResult.builder()
                            .stepOrder(order)
                            .keyword(keyword)
                            .description(desc)
                            .passed(false)
                            .durationMs(stepDuration)
                            .errorMessage(error)
                            .build());

                    finalStatus = JobStatus.FAILED;
                    break;
                }
            }

            return AutomationExecutionResult.builder()
                    .status(finalStatus)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .errorMessage(errorMessage)
                    .stepResults(stepResults)
                    .failureScreenshot(failureScreenshot)
                    .failureScreenshotName("failure-step-" + Instant.now().toEpochMilli() + ".png")
                    .diagnosticLogs(driver.getDiagnosticLogs())
                    .build();

        } finally {
            if (driver != null) {
                driver.close();
            }
        }
    }

    private void executeStep(AutomationBrowserDriver driver, AutomationStepModel step) {
        if (step.getTemplate() == null) {
            throw new IllegalArgumentException("Automation template is required");
        }
        switch (step.getTemplate()) {
            case OPEN_PAGE -> driver.openPage(step.getTextValue());
            case ENTER_TEXT -> driver.enterText(step.getLocatorValue(), step.getTextValue());
            case CLICK_ELEMENT -> driver.clickElement(step.getLocatorValue());
            case VERIFY_TEXT_EQUALS -> driver.verifyTextEquals(step.getLocatorValue(), step.getTextValue());
            case VERIFY_TEXT_CONTAINS -> driver.verifyTextContains(step.getLocatorValue(), step.getTextValue());
            case VERIFY_ELEMENT_STATE -> driver.verifyElementState(step.getLocatorValue(), step.getStateValue());
        }
    }
}
