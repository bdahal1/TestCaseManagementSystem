package com.tcms.dto;

import com.tcms.enums.ExecutionStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestExecutionDTO {
    private String executionName;
    private ExecutionStatus executionStatus;
    private int projectId;
}
