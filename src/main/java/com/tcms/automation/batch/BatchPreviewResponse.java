package com.tcms.automation.batch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchPreviewResponse {
    private Integer planId;
    private String planName;
    private String planStatus;
    private String scope;
    private boolean canExecute;
    private int maxBatchSize;
    private int totalInScope;
    private int readyCount;
    private List<BatchCasePreviewItem> readyCases;
    private List<BatchExcludedCaseItem> manualSkippedCases;
    private List<BatchExcludedCaseItem> invalidConfigCases;
    private List<BatchExcludedCaseItem> alreadyPendingCases;
    private List<BatchExcludedCaseItem> unauthorizedCases;
}
