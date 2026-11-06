package com.tcms.dto.plans;

import com.tcms.enums.*;
import java.time.*;
import java.util.List;
import java.util.Map;

public final class PlanResponses {
    private PlanResponses() {}
    public record Person(int id, String name) {}
    public record Step(Integer order, String instruction, String data, String expected) {}
    public record Snapshot(String name, TestTypes type, List<Step> steps) {}
    public record Evidence(int id, String name, String contentType, long size, Instant uploadedAt, String uploadedBy) {}
    public record Attempt(int id, TestResult status, String comment, String actualResult, String defectUrl,
                          Instant recordedAt, String recordedBy) {}
    public record Case(int id, String name, TestTypes type, List<Step> steps, TestResult status,
                       String comment, String actualResult, String defectUrl, long revision,
                       Person assignee, Instant recordedAt, String recordedBy, boolean canRecord,
                       List<Evidence> evidence, List<Attempt> history) {}
    public record Summary(int id, int projectId, String name, String release, String build, String environment, LocalDate dueDate,
                          ExecutionStatus status, String description, int total, int completed,
                          int assignedToMe, int unassigned, Map<TestResult, Long> results) {}
    public record Detail(Summary plan, List<Case> cases) {}
}
