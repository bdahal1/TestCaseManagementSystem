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
public class BatchCasePreviewItem {
    private Integer caseId;
    private String caseName;
    private TestTypes testType;
    private String assignee;
    private int stepCount;
}
