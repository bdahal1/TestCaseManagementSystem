package com.tcms.dto;

import com.tcms.models.TestCase;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class TestFolderResponseDTO {
    private String folderName;
    private int id;
    private Set<TestCase> testCases;
}
