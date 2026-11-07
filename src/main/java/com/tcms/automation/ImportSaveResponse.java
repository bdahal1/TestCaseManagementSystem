package com.tcms.automation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
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
public class ImportSaveResponse {
    private int createdCount;
    private int replacedCount;
    private List<SavedCaseSummary> savedCases;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SavedCaseSummary {
        private int id;
        private String testProjectId;
        private String name;
        private String testType;
        private String action;
    }
}
