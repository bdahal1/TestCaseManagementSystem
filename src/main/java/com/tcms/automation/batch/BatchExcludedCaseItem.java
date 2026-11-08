package com.tcms.automation.batch;

import com.tcms.enums.TestTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchExcludedCaseItem {
    private Integer caseId;
    private String caseName;
    private TestTypes testType;
    private String category; // "MANUAL", "INVALID_CONFIG", "ALREADY_PENDING", "UNAUTHORIZED"
    private String reason;
}
