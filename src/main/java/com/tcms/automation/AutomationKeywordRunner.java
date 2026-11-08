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
        return execute(definition, cancelled, AutomationProgressListener.NONE);
    }

    public AutomationExecutionResult execute(AutomationTestDefinition definition, java.util.function.BooleanSupplier cancelled,
                                             AutomationProgressListener progress) {
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
                int index = stepResults.size();
                AutomationProgressListener.publish(progress, index, "RUNNING", stepResults, driver);
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
                    AutomationProgressListener.publish(progress, index, "PASSED", stepResults, driver);
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
                    AutomationProgressListener.publish(progress, index, "FAILED", stepResults, driver);
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
        String locator = serializer.locatorReference(step);
        switch (step.getTemplate()) {
            case OPEN_PAGE -> driver.openPage(step.getTextValue());
            case ENTER_TEXT -> driver.enterText(locator, step.getTextValue());
            case CLICK_ELEMENT -> driver.clickElement(locator);
            case VERIFY_TEXT_EQUALS -> driver.verifyTextEquals(locator, step.getTextValue());
            case VERIFY_TEXT_CONTAINS -> driver.verifyTextContains(locator, step.getTextValue());
            case VERIFY_ELEMENT_STATE -> driver.verifyElementState(locator, step.getStateValue());
            case SELECT_OPTION -> driver.selectOption(locator, step.getTextValue(), step.getSelectBy());
            case SET_CHECKBOX -> driver.setCheckbox(locator, Boolean.TRUE.equals(step.getCheckedValue()));
            case PRESS_KEY -> driver.pressKey(locator, step.getKeyValue());
            case VERIFY_URL -> driver.verifyUrl(step.getTextValue(), step.getUrlComparison());
            case VERIFY_ATTRIBUTE -> driver.verifyAttribute(locator, step.getAttributeName(), step.getTextValue());
        }
    }
}
