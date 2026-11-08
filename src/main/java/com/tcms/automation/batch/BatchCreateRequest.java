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
public class BatchCreateRequest {
    private String scope; // "ALL_ELIGIBLE", "SELECTED", "RETRY_FAILED"
    private List<Integer> caseIds;
    private String batchType;
}
