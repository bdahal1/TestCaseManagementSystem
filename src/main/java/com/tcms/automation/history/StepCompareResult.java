package com.tcms.automation.history;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepCompareResult {
    private int stepOrder;
    private String description;

    // Job A outcome
    private Boolean passedA;
    private Long durationMsA;
    private String errorMessageA;
    private boolean onlyInA;

    // Job B outcome
    private Boolean passedB;
    private Long durationMsB;
    private String errorMessageB;
    private boolean onlyInB;
}
