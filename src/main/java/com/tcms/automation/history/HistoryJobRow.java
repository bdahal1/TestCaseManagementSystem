package com.tcms.automation.history;

import com.tcms.automation.JobStatus;
import com.tcms.enums.TestTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryJobRow {
    private Long jobId;
    private Integer planId;
    private String planName;
    private Long batchId;
    private Integer caseId;
    private String caseName;
    private JobStatus status;
    private TestTypes testType;
    private Instant createdAt;
    private Instant startedAt;
    private Instant finishedAt;
    private Long durationMs;
    private String errorMessage;
    private boolean hasStepResults;
    private boolean hasDiagnosticLogs;
    private Integer failureEvidenceId;
    private String createdBy;
}
