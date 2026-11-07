package com.tcms.automation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImportStepPreview {
    private int order;
    private Integer sourceLine;
    private String keyword; // Given, When, Then, And, But
    private String rawText;
    private AutomationTemplate template;
    private LocatorType locatorType;
    private String locatorValue;
    private String textValue;
    private ElementState stateValue;
    private OptionSelectBy selectBy;
    private Boolean checkedValue;
    private String keyValue;
    private UrlComparison urlComparison;
    private String attributeName;
    private boolean mapped;
    private String unsupportedSyntax;
    private String validationError;
}
