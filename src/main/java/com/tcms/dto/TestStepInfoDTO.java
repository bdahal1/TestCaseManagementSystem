package com.tcms.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestStepInfoDTO {
    String testStepDesc;
    String testExpectedOutput;
    String testStepData;
    Integer stepOrder;
    Integer testCaseId;
    Integer userId;
    Integer stepId;
}
