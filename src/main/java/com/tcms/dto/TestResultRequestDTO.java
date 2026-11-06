package com.tcms.dto;

import jakarta.validation.constraints.*;

import com.tcms.enums.TestResult;
import lombok.Data;

@Data
public class TestResultRequestDTO {
    @NotNull @Positive
    private Integer executionId;
    @NotNull @Positive
    private Integer testCaseId;
    @NotNull
    private TestResult resultStatus;
    private String resultComment;
}
