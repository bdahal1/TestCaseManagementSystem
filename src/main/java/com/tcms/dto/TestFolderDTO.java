package com.tcms.dto;

import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestFolderDTO {
    @NotBlank
    private String folderName;
    @Positive
    private int projectId;
}
