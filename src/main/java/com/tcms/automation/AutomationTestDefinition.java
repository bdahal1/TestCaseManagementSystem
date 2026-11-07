package com.tcms.automation;

import com.tcms.enums.TestTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutomationTestDefinition {
    private int caseId;
    private String caseName;
    private TestTypes testType;
    @Builder.Default
    private List<AutomationStepModel> steps = new ArrayList<>();
    @Builder.Default
    private Map<String, LocatorDefinition> locators = new HashMap<>();
    @Builder.Default
    private Map<String, String> variables = new HashMap<>();
    @Builder.Default
    private Set<String> secretKeys = new HashSet<>();
    @Builder.Default
    private Map<String, String> executionConfig = new HashMap<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocatorDefinition {
        private LocatorType type;
        private String value;
        private String description;
    }
}
