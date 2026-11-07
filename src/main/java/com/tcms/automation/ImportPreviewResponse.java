package com.tcms.automation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
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
public class ImportPreviewResponse {
    private String fileName;
    private String fileType; // FEATURE or KEYWORD_JSON
    @Builder.Default
    private List<ImportCasePreview> cases = new ArrayList<>();
    private boolean hasErrors;
    private boolean canSave;
    @Builder.Default
    private List<String> globalErrors = new ArrayList<>();
    private int totalCases;
    private int totalSteps;
}
