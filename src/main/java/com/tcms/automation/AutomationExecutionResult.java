package com.tcms.automation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutomationExecutionResult {
    private JobStatus status;
    private long durationMs;
    private String errorMessage;
    @Builder.Default
    private List<StepResult> stepResults = new ArrayList<>();
    private byte[] failureScreenshot;
    private String failureScreenshotName;
    private String diagnosticLogs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepResult {
        private int stepOrder;
        private String keyword;
        private String description;
        private boolean passed;
        private long durationMs;
        private String errorMessage;
        private Integer evidenceId;
    }
}
