package com.tcms.automation;

import java.util.List;

@FunctionalInterface
public interface AutomationProgressListener {
    AutomationProgressListener NONE = (index, status, results, driver) -> {};

    void update(int index, String status, List<AutomationExecutionResult.StepResult> results, AutomationBrowserDriver driver);

    static void publish(AutomationProgressListener listener, int index, String status,
                        List<AutomationExecutionResult.StepResult> results, AutomationBrowserDriver driver) {
        try {
            listener.update(index, status, List.copyOf(results), driver);
        } catch (RuntimeException ignored) {
            // Optional preview transport must not change the test outcome.
        }
    }
}
