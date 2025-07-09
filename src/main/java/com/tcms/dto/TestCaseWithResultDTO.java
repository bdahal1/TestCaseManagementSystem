package com.tcms.dto;

import com.tcms.enums.TestResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TestCaseWithResultDTO {
    private int id;
    private String testName;
    private TestResult resultStatus;    // enum
    private String resultComment;       // optional
}