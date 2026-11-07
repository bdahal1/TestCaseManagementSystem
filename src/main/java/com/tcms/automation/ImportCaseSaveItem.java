package com.tcms.automation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.tcms.enums.TestTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImportCaseSaveItem {
    private ImportAction action; // CREATE_NEW or REPLACE_EXISTING
    private Integer targetCaseId; // required if REPLACE_EXISTING
    private String caseName;
    private TestTypes testType;
    private List<AutomationStepModel> steps;
}
