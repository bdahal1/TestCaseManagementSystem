package com.tcms.dto.reports;

import com.tcms.enums.*;
import java.time.*;
import java.util.*;

public record ReportResponse(Instant generatedAt, LocalDate from, LocalDate to, List<String> environments, List<String> releases,
                             Library library, Execution execution, List<Plan> plans, List<Day> activity,
                             List<CaseRow> cases, List<Workload> workload) {
    public record Library(int total, Map<CasePriority,Long> priorities, Map<AutomationStatus,Long> automation,
                          Map<TestTypes,Long> types, int unowned, long estimatedMinutes, int estimatesDefined) {}
    public record Execution(int activePlans, int closedPlans, int draftPlans, long total, long completed,
                            Double passRate, Double completionRate, int testedCases, int neverRunCases, int linkedDefects,
                            Map<TestResult,Long> results) {}
    public record Plan(int id, String name, String release, String build, String environment, ExecutionStatus status,
                       int total, long completed, Double passRate, int unassigned, Map<TestResult,Long> results) {}
    public record Day(LocalDate date, Map<TestResult,Long> results) {}
    public record CaseRow(int id, String key, String name, CasePriority priority, AutomationStatus automationStatus,
                          String component, String owner, Integer estimatedMinutes, TestResult result,
                          Integer planId, String planName, Instant recordedAt, String defectUrl, boolean definitionChanged) {}
    public record Workload(String tester, int total, int remaining, int blocked, long estimatedRemainingMinutes, int missingEstimates) {}
}
