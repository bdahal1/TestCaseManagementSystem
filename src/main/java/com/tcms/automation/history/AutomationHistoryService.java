package com.tcms.automation.history;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.tcms.automation.*;
import com.tcms.automation.AutomationExecutionResult.StepResult;
import com.tcms.enums.TestTypes;
import com.tcms.repositories.TestExecutionRepository;
import com.tcms.security.ProjectAccess;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class AutomationHistoryService {

    private final AutomationJobRepository jobRepository;
    private final TestExecutionRepository planRepository;
    private final AutomationJobService jobService;
    private final AutomationStepSerializer serializer;
    private final ProjectAccess projectAccess;
    private final ObjectMapper mapper;

    public AutomationHistoryService(AutomationJobRepository jobRepository,
                                    TestExecutionRepository planRepository,
                                    AutomationJobService jobService,
                                    AutomationStepSerializer serializer,
                                    ProjectAccess projectAccess,
                                    ObjectMapper mapper) {
        this.jobRepository = jobRepository;
        this.planRepository = planRepository;
        this.jobService = jobService;
        this.serializer = serializer;
        this.projectAccess = projectAccess;
        this.mapper = mapper;
    }

    public HistoryPageResponse getHistory(
            int projectId,
            Integer planId,
            String statusStr,
            String caseNameContains,
            Instant from,
            Instant to,
            int page,
            int size) {

        requireProjectAccess(projectId);

        if (size < 1 || size > 100) size = 20;
        if (page < 0) page = 0;

        JobStatus status = parseStatus(statusStr);

        String nameFragment = (caseNameContains == null || caseNameContains.isBlank())
                ? null
                : "%" + caseNameContains.toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";

        Page<AutomationJob> jobPage = jobRepository.findHistory(
                projectId, planId, status, from, to, nameFragment,
                PageRequest.of(page, size));

        Map<Integer, String> planNameCache = new HashMap<>();

        List<HistoryJobRow> rows = new ArrayList<>();
        for (AutomationJob job : jobPage.getContent()) {
            String planName = planNameCache.computeIfAbsent(job.getPlanId(), id -> {
                var plan = planRepository.findById(id);
                return plan != null ? plan.getExecutionName() : "#" + id;
            });
            rows.add(toRow(job, planName));
        }

        return HistoryPageResponse.builder()
                .content(rows)
                .totalElements(jobPage.getTotalElements())
                .page(page)
                .size(size)
                .totalPages(jobPage.getTotalPages())
                .build();
    }

    public Map<String, Object> getJobDetail(int projectId, Long jobId) {
        requireProjectAccess(projectId);
        AutomationJob job = loadAndVerify(projectId, jobId);

        List<StepResult> stepResults = Collections.emptyList();
        if (job.getStepResultsJson() != null && !job.getStepResultsJson().isBlank()) {
            try {
                stepResults = mapper.readValue(job.getStepResultsJson(), new TypeReference<List<StepResult>>() {});
            } catch (Exception ignored) {}
        }

        var secrets = jobService.loadProjectVariablesAndSecrets(projectId);
        List<String> secretValues = secrets.secretKeys().stream().map(secrets.variables()::get).filter(Objects::nonNull).toList();

        String caseName = extractCaseName(job);

        String planName = null;
        var plan = planRepository.findById(job.getPlanId());
        if (plan != null) planName = plan.getExecutionName();

        String errorMessage = GherkinStrings.redact(job.getErrorMessage(), secretValues);
        String diagnosticLogs = GherkinStrings.redact(job.getDiagnosticLogs(), secretValues);

        List<StepResult> redactedStepResults = new ArrayList<>();
        for (StepResult sr : stepResults) {
            redactedStepResults.add(StepResult.builder()
                    .stepOrder(sr.getStepOrder())
                    .keyword(sr.getKeyword())
                    .description(GherkinStrings.redact(sr.getDescription(), secretValues))
                    .passed(sr.isPassed())
                    .durationMs(sr.getDurationMs())
                    .errorMessage(GherkinStrings.redact(sr.getErrorMessage(), secretValues))
                    .evidenceId(sr.getEvidenceId())
                    .build());
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("jobId",             job.getId());
        resp.put("planId",            job.getPlanId());
        resp.put("planName",          planName);
        resp.put("batchId",           job.getBatchId());
        resp.put("caseId",            job.getCaseId());
        resp.put("caseName",          caseName);
        resp.put("projectId",         job.getProjectId());
        resp.put("status",            job.getJobStatus());
        resp.put("testType",          job.getTestType());
        resp.put("createdAt",         job.getCreatedAt());
        resp.put("startedAt",         job.getStartedAt());
        resp.put("finishedAt",        job.getFinishedAt());
        resp.put("durationMs",        computeDuration(job));
        resp.put("errorMessage",      errorMessage);
        resp.put("diagnosticLogs",    diagnosticLogs);
        resp.put("stepResults",       redactedStepResults);
        resp.put("failureEvidenceId", job.getFailureEvidenceId());
        resp.put("createdBy",         job.getCreatedBy());
        return resp;
    }

    public List<AttemptSummary> getAttempts(int projectId, int planId, int caseId) {
        requireProjectAccess(projectId);
        var plan = planRepository.findById(planId);
        if (plan == null || !Objects.equals(plan.getProjects().getId(), projectId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found in this project");
        }
        List<AutomationJob> jobs = jobRepository.findByPlanIdAndCaseIdAndProjectIdOrderByIdAsc(planId, caseId, projectId);
        List<AttemptSummary> attempts = new ArrayList<>();
        for (int i = 0; i < jobs.size(); i++) {
            AutomationJob job = jobs.get(i);
            attempts.add(AttemptSummary.builder()
                    .jobId(job.getId())
                    .attemptNumber(i + 1)
                    .status(job.getJobStatus())
                    .createdAt(job.getCreatedAt())
                    .finishedAt(job.getFinishedAt())
                    .durationMs(computeDuration(job))
                    .build());
        }
        return attempts;
    }

    public CompareAttemptResponse compare(int projectId, Long jobIdA, Long jobIdB) {
        requireProjectAccess(projectId);
        if (Objects.equals(jobIdA, jobIdB)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot compare a job with itself");
        }

        AutomationJob jobA = loadAndVerify(projectId, jobIdA);
        AutomationJob jobB = loadAndVerify(projectId, jobIdB);

        if (!Objects.equals(jobA.getPlanId(), jobB.getPlanId()) || !Objects.equals(jobA.getCaseId(), jobB.getCaseId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attempts must belong to the same test plan and test case");
        }

        var secrets = jobService.loadProjectVariablesAndSecrets(projectId);
        List<String> secretValues = secrets.secretKeys().stream().map(secrets.variables()::get).filter(Objects::nonNull).toList();

        List<StepSnapshot> stepsA = loadStepSnapshots(jobA, secretValues);
        List<StepSnapshot> stepsB = loadStepSnapshots(jobB, secretValues);

        List<StepCompareResult> stepRows = new ArrayList<>();
        boolean hasChangedSteps = false;

        boolean[] usedB = new boolean[stepsB.size()];

        for (StepSnapshot sa : stepsA) {
            int matchIdx = -1;
            for (int j = 0; j < stepsB.size(); j++) {
                if (!usedB[j] && Objects.equals(sa.description, stepsB.get(j).description)) {
                    matchIdx = j;
                    break;
                }
            }

            if (matchIdx >= 0) {
                usedB[matchIdx] = true;
                StepSnapshot sb = stepsB.get(matchIdx);
                stepRows.add(StepCompareResult.builder()
                        .stepOrder(sa.order)
                        .description(sa.description)
                        .passedA(sa.passed)
                        .durationMsA(sa.durationMs)
                        .errorMessageA(sa.errorMessage)
                        .onlyInA(false)
                        .passedB(sb.passed)
                        .durationMsB(sb.durationMs)
                        .errorMessageB(sb.errorMessage)
                        .onlyInB(false)
                        .build());
            } else {
                hasChangedSteps = true;
                stepRows.add(StepCompareResult.builder()
                        .stepOrder(sa.order)
                        .description(sa.description)
                        .passedA(sa.passed)
                        .durationMsA(sa.durationMs)
                        .errorMessageA(sa.errorMessage)
                        .onlyInA(true)
                        .passedB(null)
                        .durationMsB(null)
                        .errorMessageB(null)
                        .onlyInB(false)
                        .build());
            }
        }

        for (int j = 0; j < stepsB.size(); j++) {
            if (!usedB[j]) {
                hasChangedSteps = true;
                StepSnapshot sb = stepsB.get(j);
                stepRows.add(StepCompareResult.builder()
                        .stepOrder(sb.order)
                        .description(sb.description)
                        .passedA(null)
                        .durationMsA(null)
                        .errorMessageA(null)
                        .onlyInA(false)
                        .passedB(sb.passed)
                        .durationMsB(sb.durationMs)
                        .errorMessageB(sb.errorMessage)
                        .onlyInB(true)
                        .build());
            }
        }

        String caseName = extractCaseName(jobA);

        return CompareAttemptResponse.builder()
                .jobIdA(jobIdA)
                .jobIdB(jobIdB)
                .planId(jobA.getPlanId())
                .caseId(jobA.getCaseId())
                .caseName(caseName)
                .statusA(jobA.getJobStatus() != null ? jobA.getJobStatus().name() : null)
                .statusB(jobB.getJobStatus() != null ? jobB.getJobStatus().name() : null)
                .durationMsA(computeDuration(jobA))
                .durationMsB(computeDuration(jobB))
                .steps(stepRows)
                .hasChangedSteps(hasChangedSteps)
                .build();
    }

    private record StepSnapshot(int order, String description, Boolean passed, Long durationMs, String errorMessage) {}

    private List<StepSnapshot> loadStepSnapshots(AutomationJob job, List<String> secretValues) {
        List<StepSnapshot> list = new ArrayList<>();
        Map<Integer, StepResult> resultsByOrder = new HashMap<>();

        if (job.getStepResultsJson() != null && !job.getStepResultsJson().isBlank()) {
            try {
                List<StepResult> results = mapper.readValue(job.getStepResultsJson(), new TypeReference<List<StepResult>>() {});
                if (results != null) {
                    for (StepResult r : results) {
                        resultsByOrder.put(r.getStepOrder(), r);
                    }
                }
            } catch (Exception ignored) {}
        }

        if (job.getTestDefinition() != null && !job.getTestDefinition().isBlank()) {
            try {
                AutomationTestDefinition def = mapper.readValue(job.getTestDefinition(), AutomationTestDefinition.class);
                if (def.getSteps() != null) {
                    for (int i = 0; i < def.getSteps().size(); i++) {
                        AutomationStepModel step = def.getSteps().get(i);
                        int order = step.getOrder() > 0 ? step.getOrder() : i + 1;
                        String desc = def.getTestType() == TestTypes.CUCUMBER_AUTOMATION
                                ? serializer.toGherkinStep(step)
                                : serializer.toKeywordDescription(step);
                        desc = GherkinStrings.redact(desc, secretValues);

                        StepResult res = resultsByOrder.get(order);
                        Boolean passed = res != null ? res.isPassed() : null;
                        Long dur = res != null ? (long) res.getDurationMs() : null;
                        String err = res != null ? GherkinStrings.redact(res.getErrorMessage(), secretValues) : null;

                        list.add(new StepSnapshot(order, desc, passed, dur, err));
                    }
                    return list;
                }
            } catch (Exception ignored) {}
        }

        if (!resultsByOrder.isEmpty()) {
            List<StepResult> sorted = new ArrayList<>(resultsByOrder.values());
            sorted.sort(Comparator.comparingInt(StepResult::getStepOrder));
            for (StepResult r : sorted) {
                list.add(new StepSnapshot(
                        r.getStepOrder(),
                        GherkinStrings.redact(r.getDescription(), secretValues),
                        r.isPassed(),
                        (long) r.getDurationMs(),
                        GherkinStrings.redact(r.getErrorMessage(), secretValues)
                ));
            }
        }

        return list;
    }

    private void requireProjectAccess(int projectId) {
        if (!projectAccess.project(projectId)) {
            throw new org.springframework.security.access.AccessDeniedException("Project access required");
        }
    }

    private AutomationJob loadAndVerify(int projectId, Long jobId) {
        AutomationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
        if (!Objects.equals(job.getProjectId(), projectId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found");
        }
        return job;
    }

    private static JobStatus parseStatus(String statusStr) {
        if (statusStr == null || statusStr.isBlank()) return null;
        try {
            return JobStatus.valueOf(statusStr.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown status: " + statusStr);
        }
    }

    private static Long computeDuration(AutomationJob job) {
        if (job.getStartedAt() == null || job.getFinishedAt() == null) return null;
        return job.getFinishedAt().toEpochMilli() - job.getStartedAt().toEpochMilli();
    }

    String extractCaseName(AutomationJob job) {
        if (job.getCaseName() != null && !job.getCaseName().isBlank()) {
            return job.getCaseName();
        }
        if (job.getTestDefinition() != null) {
            try {
                var node = mapper.readTree(job.getTestDefinition());
                var nameNode = node.get("caseName");
                if (nameNode != null && !nameNode.isNull() && !nameNode.textValue().isBlank()) {
                    return nameNode.textValue();
                }
            } catch (Exception ignored) {}
        }
        if (job.getCaseExecution() != null && job.getCaseExecution().getTestCase() != null) {
            return job.getCaseExecution().getTestCase().getTestName();
        }
        return "Case #" + job.getCaseId();
    }

    private HistoryJobRow toRow(AutomationJob job, String planName) {
        String caseName = extractCaseName(job);
        String err = job.getErrorMessage();
        if (err != null && err.length() > 500) err = err.substring(0, 500) + "…";
        return HistoryJobRow.builder()
                .jobId(job.getId())
                .planId(job.getPlanId())
                .planName(planName)
                .batchId(job.getBatchId())
                .caseId(job.getCaseId())
                .caseName(caseName)
                .status(job.getJobStatus())
                .testType(job.getTestType())
                .createdAt(job.getCreatedAt())
                .startedAt(job.getStartedAt())
                .finishedAt(job.getFinishedAt())
                .durationMs(computeDuration(job))
                .errorMessage(err)
                .hasStepResults(job.getStepResultsJson() != null && !job.getStepResultsJson().isBlank())
                .hasDiagnosticLogs(job.getDiagnosticLogs() != null && !job.getDiagnosticLogs().isBlank())
                .failureEvidenceId(job.getFailureEvidenceId())
                .createdBy(job.getCreatedBy())
                .build();
    }
}
