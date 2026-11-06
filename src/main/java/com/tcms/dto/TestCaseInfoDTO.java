package com.tcms.dto;

import jakarta.validation.constraints.*;

import com.tcms.enums.TestTypes;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestCaseInfoDTO {
    @NotBlank
    String testName;
    @NotNull @Positive
    Integer userId;
    @NotNull @Positive
    Integer projectId;
    Integer[] selectedTags;
    TestTypes testType;
}
