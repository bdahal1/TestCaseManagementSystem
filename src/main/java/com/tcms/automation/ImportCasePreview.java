package com.tcms.automation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.tcms.enums.TestTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImportCasePreview {
    private String name;
    private TestTypes testType;
    private Integer sourceLine;
    @Builder.Default
    private List<ImportStepPreview> steps = new ArrayList<>();
    private boolean conflict;
    private Integer existingCaseId;
    private String existingCaseProjectId;
    private String existingCaseName;
    @Builder.Default
    private ImportAction action = ImportAction.CREATE_NEW;
    private String unsupportedSyntax;
    private String validationError;
    private int unmappedCount;
    private int errorCount;
}
