package com.tcms.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestCaseInfoDTO {
    String testName;
    int testCreatedBy;
    int projectId;
}
