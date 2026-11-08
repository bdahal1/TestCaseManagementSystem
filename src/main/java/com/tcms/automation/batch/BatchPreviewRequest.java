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
public class BatchPreviewRequest {
    private String scope; // "ALL_ELIGIBLE" or "SELECTED"
    private List<Integer> caseIds;
}
