package com.tcms.automation;

import io.cucumber.core.cli.Main;
import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.*;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

@Component
public class AutomationCucumberRunner {
    private final AutomationStepSerializer serializer;

    public AutomationCucumberRunner(AutomationStepSerializer serializer) {
        this.serializer = serializer;
    }

    public static class StepCollectorPlugin implements ConcurrentEventListener {
        private static final ThreadLocal<CollectorState> CURRENT = ThreadLocal.withInitial(CollectorState::new);
        private static class CollectorState {
            final List<AutomationExecutionResult.StepResult> collected = new ArrayList<>();
            Throwable lastError;
            AutomationBrowserDriver driver;
            AutomationProgressListener progress = AutomationProgressListener.NONE;
        }

        public static void reset() {
            CURRENT.set(new CollectorState());
        }

        public static List<AutomationExecutionResult.StepResult> getResults() {
            return new ArrayList<>(CURRENT.get().collected);
        }

        public static Throwable getLastError() {
            return CURRENT.get().lastError;
        }

        @Override
        public void setEventPublisher(EventPublisher publisher) {
            publisher.registerHandlerFor(TestStepStarted.class, event -> {
                var state = CURRENT.get();
                // After a failure Cucumber emits skipped events; keep the failing step highlighted.
                if (event.getTestStep() instanceof PickleStepTestStep && state.lastError == null && state.driver != null)
                    AutomationProgressListener.publish(state.progress, state.collected.size(), "RUNNING", state.collected, state.driver);
            });
            publisher.registerHandlerFor(TestStepFinished.class, this::handleStepFinished);
        }

        private void handleStepFinished(TestStepFinished event) {
            if (event.getTestStep() instanceof PickleStepTestStep step) {
                var state = CURRENT.get();
                boolean passed = event.getResult().getStatus() == Status.PASSED;
                long durationMs = event.getResult().getDuration().toMillis();
                String error = null;
                if (!passed && event.getResult().getError() != null) {
                    error = event.getResult().getError().getMessage();
                    state.lastError = event.getResult().getError();
                }

                int index = state.collected.size();
                state.collected.add(AutomationExecutionResult.StepResult.builder()
                        .stepOrder(index + 1)
                        .keyword(step.getStep().getKeyword())
                        .description(step.getStep().getKeyword() + " " + step.getStep().getText())
                        .passed(passed)
                        .durationMs(durationMs)
                        .errorMessage(error)
                        .build());
                if (event.getResult().getStatus() != Status.SKIPPED && state.driver != null)
                    AutomationProgressListener.publish(state.progress, index, passed ? "PASSED" : "FAILED", state.collected, state.driver);
            }
        }
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
        Path tempDir = null;
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

            AutomationCucumberStepDefinitions.setDriver(driver);
            StepCollectorPlugin.reset();
            StepCollectorPlugin.CURRENT.get().driver = driver;
            StepCollectorPlugin.CURRENT.get().progress = progress;

            tempDir = Files.createTempDirectory("tcms-cucumber-");
            Path featureFile = tempDir.resolve("scenario.feature");
            String gherkin = serializer.toGherkinScenario(definition.getCaseName(), definition.getSteps());
            Files.writeString(featureFile, gherkin, StandardCharsets.UTF_8);

            String[] args = new String[]{
                    featureFile.toAbsolutePath().toString(),
                    "--glue", "com.tcms.automation",
                    "--plugin", StepCollectorPlugin.class.getName(),
                    "--monochrome"
            };

            byte exitStatus = Main.run(args, Thread.currentThread().getContextClassLoader());
            List<AutomationExecutionResult.StepResult> stepResults = StepCollectorPlugin.getResults();
            Throwable lastError = StepCollectorPlugin.getLastError();

            byte[] failureScreenshot = null;
            String errorMessage = null;
            JobStatus status = JobStatus.COMPLETED;

            if (exitStatus != 0 || lastError != null || stepResults.stream().anyMatch(s -> !s.isPassed())) {
                failureScreenshot = driver.takeScreenshot();
                if (lastError != null) {
                    String cleanError = driver.redact(lastError.getMessage());
                    errorMessage = cleanError;
                } else {
                    errorMessage = "Cucumber scenario execution failed";
                }
                status = JobStatus.FAILED;
            }

            return AutomationExecutionResult.builder()
                    .status(status)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .errorMessage(errorMessage)
                    .stepResults(stepResults)
                    .failureScreenshot(failureScreenshot)
                    .failureScreenshotName("cucumber-failure-" + Instant.now().toEpochMilli() + ".png")
                    .diagnosticLogs(driver.getDiagnosticLogs())
                    .build();

        } catch (Exception e) {
            return AutomationExecutionResult.builder()
                    .status(JobStatus.INFRASTRUCTURE_ERROR)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .errorMessage("Execution failed with internal error: " + e.getMessage())
                    .diagnosticLogs(driver != null ? driver.getDiagnosticLogs() : e.getMessage())
                    .build();
        } finally {
            StepCollectorPlugin.CURRENT.remove();
            AutomationCucumberStepDefinitions.clearDriver();
            if (driver != null) {
                driver.close();
            }
            if (tempDir != null) {
                deleteRecursively(tempDir.toFile());
            }
        }
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}
