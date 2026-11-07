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
public class AutomationStepModel {
    private int order;
    private String keyword; // Given, When, Then, And, But
    private AutomationTemplate template;
    private LocatorType locatorType;
    private String locatorValue;
    private String textValue;
    private ElementState stateValue;
}
