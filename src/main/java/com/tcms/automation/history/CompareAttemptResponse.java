package com.tcms.automation.history;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompareAttemptResponse {
    private Long jobIdA;
    private Long jobIdB;
    private Integer planId;
    private Integer caseId;
    private String caseName;
    private String statusA;
    private String statusB;
    private Long durationMsA;
    private Long durationMsB;
    private List<StepCompareResult> steps;
    private boolean hasChangedSteps;
}
