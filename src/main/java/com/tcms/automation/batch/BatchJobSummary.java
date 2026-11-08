package com.tcms.automation.batch;

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
public class BatchJobSummary {
    private Long jobId;
    private Integer caseId;
    private String caseName;
    private TestTypes testType;
    private JobStatus status;
    private boolean canCancel;
    private Long durationMs;
    private String errorMessage;
    private String stepSummary;
    private Instant startedAt;
    private Instant finishedAt;
    private String assignedWorkerId;
}
