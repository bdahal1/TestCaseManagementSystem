package com.tcms.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class TestExecutionResponseDTO {
    private String executionName;
    private int id;
    private Set<TestCaseWithResultDTO> testCases;
}
