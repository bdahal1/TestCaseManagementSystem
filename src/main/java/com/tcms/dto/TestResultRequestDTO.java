package com.tcms.dto;

import com.tcms.enums.TestResult;
import lombok.Data;

@Data
public class TestResultRequestDTO {
    private Integer executionId;
    private Integer testCaseId;
    private TestResult resultStatus;
    private String resultComment;
}
