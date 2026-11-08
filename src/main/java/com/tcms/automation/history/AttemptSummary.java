package com.tcms.automation.history;

import com.tcms.automation.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttemptSummary {
    private Long jobId;
    private int attemptNumber;
    private JobStatus status;
    private Instant createdAt;
    private Instant finishedAt;
    private Long durationMs;
}
