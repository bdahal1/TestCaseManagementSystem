package com.tcms.dto;

import com.tcms.enums.TestTypes;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestCaseInfoDTO {
    String testName;
    Integer userId;
    Integer projectId;
    Integer[] selectedTags;
    TestTypes testType;
}
