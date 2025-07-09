package com.tcms.dto;

import com.tcms.models.TestCase;
import lombok.Data;

import java.util.Set;

@Data
public class TestFolderResponseDTO {
    private String folderName;
    private int id;
    private Set<TestCase> testCases;
}
