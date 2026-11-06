package com.tcms.dto;

import jakarta.validation.constraints.*;

import com.tcms.enums.ExecutionStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestExecutionDTO {
    @NotBlank
    private String executionName;
    private ExecutionStatus executionStatus;
    @Positive
    private int projectId;
}
