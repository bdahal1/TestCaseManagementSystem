package com.tcms.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestStepInfoDTO {
    String testStepDesc;
    String testExpectedOutput;
    String testRemarks;
    Integer testCaseId;
    Integer userId;
}
