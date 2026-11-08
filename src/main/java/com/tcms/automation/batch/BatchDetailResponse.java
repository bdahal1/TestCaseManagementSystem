package com.tcms.automation.batch;

import com.tcms.automation.BatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchDetailResponse {
    private Long batchId;
    private Integer planId;
    private String planName;
    private Integer projectId;
    private String batchType;
    private BatchStatus status;
    private int totalCases;
    private Map<String, Integer> counts;
    private int percentComplete;
    private boolean finished;
    private boolean canCancel;
    private boolean canRetryFailed;

    public boolean isFinished() {
        return finished;
    }

    public boolean getIsFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public void setIsFinished(boolean isFinished) {
        this.finished = isFinished;
    }
    private Instant createdAt;
    private Instant finishedAt;
    private String createdBy;
    private Instant cancelledAt;
    private String cancelledBy;
    private List<BatchJobSummary> jobs;
}
