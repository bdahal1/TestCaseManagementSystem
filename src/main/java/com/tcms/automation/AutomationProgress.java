package com.tcms.automation;

import java.util.List;

/** Latest observation only; progress never records an execution result. */
public record AutomationProgress(long sequence, int currentStepIndex, String stepStatus,
                                 List<AutomationExecutionResult.StepResult> completedSteps, byte[] screenshot) {}
