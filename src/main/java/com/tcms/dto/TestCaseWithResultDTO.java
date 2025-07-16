package com.tcms.dto;

import com.tcms.enums.TestResult;
import com.tcms.enums.TestTypes;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TestCaseWithResultDTO {
    private int id;
    private String testName;
    private Set<TestStepInfoDTO> testSteps;
    private TestResult resultStatus;    // enum
    private TestTypes testType;
    private String resultComment;       // optional
}