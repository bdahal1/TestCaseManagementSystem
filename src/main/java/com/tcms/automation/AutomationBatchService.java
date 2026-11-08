package com.tcms.automation;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.tcms.automation.batch.*;
import com.tcms.enums.ExecutionStatus;
import com.tcms.enums.TestTypes;
import com.tcms.models.TestCase;
import com.tcms.models.TestCaseExecutions;
import com.tcms.models.TestExecutions;
import com.tcms.repositories.TestCaseExecutionRepository;
import com.tcms.repositories.TestExecutionRepository;
import com.tcms.security.ProjectAccess;
import com.tcms.services.ExecutionPolicy;
import com.tcms.services.TestCaseSnapshots;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@Service
// Retry reads its source batch before locking the plan. Each pending-job check must see commits
// made while it waited for that lock (including on MySQL, whose default is REPEATABLE_READ).
@Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
public class AutomationBatchService {
    public static final int MAX_BATCH_SIZE = 100;
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    private final AutomationBatchRepository batchRepository;
    private final AutomationJobRepository jobRepository;
    private final TestExecutionRepository planRepository;
    private final TestCaseExecutionRepository linkRepository;
    private final AutomationJobService jobService;
    private final ProjectAccess projectAccess;
    private final ExecutionPolicy policy;
    private final TestCaseSnapshots snapshots;
    private final AutomationStepSerializer serializer;
    private final AutomationValidator validator;
    private final ObjectMapper mapper;

    public AutomationBatchService(AutomationBatchRepository batchRepository,
                                  AutomationJobRepository jobRepository,
                                  TestExecutionRepository planRepository,
                                  TestCaseExecutionRepository linkRepository,
                                  AutomationJobService jobService,
                                  ProjectAccess projectAccess,
                                  ExecutionPolicy policy,
                                  TestCaseSnapshots snapshots,
                                  AutomationStepSerializer serializer,
                                  AutomationValidator validator,
                                  ObjectMapper mapper) {
        this.batchRepository = batchRepository;
        this.jobRepository = jobRepository;
        this.planRepository = planRepository;
        this.linkRepository = linkRepository;
        this.jobService = jobService;
        this.projectAccess = projectAccess;
        this.policy = policy;
        this.snapshots = snapshots;
        this.serializer = serializer;
        this.validator = validator;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public BatchPreviewResponse previewBatch(int planId, List<Integer> requestedCaseIds, String scope) {
        TestExecutions plan = planRepository.findById(planId);
        if (plan == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Test plan not found");
        }

        if (!projectAccess.project(plan.getProjects().getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Project access required");
        }

        List<TestCaseExecutions> allLinks = linkRepository.findByTestExecutionsId(planId);
        Map<Integer, TestCaseExecutions> linksByCaseId = new HashMap<>();
        for (TestCaseExecutions link : allLinks) {
            linksByCaseId.put(link.getTestCase().getId(), link);
        }

        List<TestCaseExecutions> inScopeLinks = new ArrayList<>();
        String normalizedScope = scope == null || scope.isBlank()
                ? requestedCaseIds != null && !requestedCaseIds.isEmpty() ? "SELECTED" : "ALL_ELIGIBLE"
                : scope.toUpperCase(Locale.ROOT);
        if (!Set.of("SELECTED", "ALL_ELIGIBLE").contains(normalizedScope))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose SELECTED or ALL_ELIGIBLE scope");
        boolean selectedScope = normalizedScope.equals("SELECTED");
        if (!selectedScope && requestedCaseIds != null && !requestedCaseIds.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use SELECTED scope when specifying case IDs");
        if (requestedCaseIds != null && (requestedCaseIds.stream().anyMatch(Objects::isNull) || new HashSet<>(requestedCaseIds).size() != requestedCaseIds.size()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Case IDs must be unique and non-null");

        if (selectedScope) {
            if (requestedCaseIds == null || requestedCaseIds.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No cases specified for selected batch preview");
            }
            for (Integer requestedId : requestedCaseIds) {
                TestCaseExecutions link = linksByCaseId.get(requestedId);
                if (link == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Case #" + requestedId + " is not part of this plan");
                }
                inScopeLinks.add(link);
            }
        } else {
            inScopeLinks.addAll(allLinks);
        }

        var locatorsMap = jobService.loadProjectLocators(plan.getProjects().getId());
        var secrets = jobService.loadProjectVariablesAndSecrets(plan.getProjects().getId());

        List<BatchCasePreviewItem> readyCases = new ArrayList<>();
        List<BatchExcludedCaseItem> manualSkipped = new ArrayList<>();
        List<BatchExcludedCaseItem> invalidConfig = new ArrayList<>();
        List<BatchExcludedCaseItem> alreadyPending = new ArrayList<>();
        List<BatchExcludedCaseItem> unauthorized = new ArrayList<>();

        for (TestCaseExecutions link : inScopeLinks) {
            TestCase tc = link.getTestCase();
            var snapshot = snapshots.read(link);
            String caseName = snapshot.name() != null ? snapshot.name() : "Case #" + tc.getId();
            String assignee = link.getAssignee() != null
                    ? link.getAssignee().getFirstName() + " " + link.getAssignee().getLastName()
                    : "Unassigned";

            // 1. Manual check
            if (snapshot.type() != TestTypes.CUCUMBER_AUTOMATION && snapshot.type() != TestTypes.KEYWORD_DRIVEN) {
                manualSkipped.add(BatchExcludedCaseItem.builder()
                        .caseId(tc.getId())
                        .caseName(caseName)
                        .testType(snapshot.type())
                        .category("MANUAL")
                        .reason("Manual test case (automated execution is only supported for Cucumber Automation and Keyword Driven).")
                        .build());
                continue;
            }

            // 2. Assignment & permissions check
            if (!policy.canRecord(link)) {
                unauthorized.add(BatchExcludedCaseItem.builder()
                        .caseId(tc.getId())
                        .caseName(caseName)
                        .testType(snapshot.type())
                        .category("UNAUTHORIZED")
                        .reason("Under plan assignment rules, you cannot record results for this case (assigned to " + assignee + ").")
                        .build());
                continue;
            }

            // 3. Already queued/running check (one-pending-job-per-case rule)
            if (jobRepository.existsByCaseExecutionIdAndJobStatusIn(link.getId(), List.of(JobStatus.QUEUED, JobStatus.RUNNING))) {
                alreadyPending.add(BatchExcludedCaseItem.builder()
                        .caseId(tc.getId())
                        .caseName(caseName)
                        .testType(snapshot.type())
                        .category("ALREADY_PENDING")
                        .reason("An automation run is already queued or running for this case. Cancel it before starting another.")
                        .build());
                continue;
            }

            // Preview reads the preserved definition without capturing or changing it.
            List<AutomationStepModel> steps;
            try {
                steps = jobService.readAutomationSteps(snapshot);
            } catch (ResponseStatusException error) {
                invalidConfig.add(BatchExcludedCaseItem.builder().caseId(tc.getId()).caseName(caseName)
                        .testType(snapshot.type()).category("INVALID_CONFIG").reason(error.getReason()).build());
                continue;
            }

            var validation = validator.validate(steps, locatorsMap);
            if (!validation.isValid()) {
                invalidConfig.add(BatchExcludedCaseItem.builder()
                        .caseId(tc.getId())
                        .caseName(caseName)
                        .testType(snapshot.type())
                        .category("INVALID_CONFIG")
                        .reason("Automation configuration error: " + jobService.redactConfigurationError(validation.getJoinedMessage(), secrets.variables(), secrets.secretKeys()))
                        .build());
                continue;
            }

            // 5. Ready to run
            readyCases.add(BatchCasePreviewItem.builder()
                    .caseId(tc.getId())
                    .caseName(caseName)
                    .testType(snapshot.type())
                    .assignee(assignee)
                    .stepCount(steps.size())
                    .build());
        }

        boolean activePlan = plan.getExecutionStatus() == ExecutionStatus.IN_PROGRESS;

        return BatchPreviewResponse.builder()
                .planId(plan.getId())
                .planName(plan.getExecutionName())
                .planStatus(plan.getExecutionStatus().name())
                .scope(selectedScope ? "SELECTED" : "ALL_ELIGIBLE")
                .canExecute(activePlan && !readyCases.isEmpty() && readyCases.size() <= MAX_BATCH_SIZE)
                .maxBatchSize(MAX_BATCH_SIZE)
                .totalInScope(inScopeLinks.size())
                .readyCount(readyCases.size())
                .readyCases(readyCases)
                .manualSkippedCases(manualSkipped)
                .invalidConfigCases(invalidConfig)
                .alreadyPendingCases(alreadyPending)
                .unauthorizedCases(unauthorized)
                .build();
    }

    public AutomationBatch createBatch(int planId, List<Integer> caseIds, String batchType) {
        TestExecutions plan = planRepository.findForUpdate(planId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Test plan not found"));
        entityManager.refresh(plan, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

        if (!projectAccess.project(plan.getProjects().getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Project access required");
        }

        if (plan.getExecutionStatus() != ExecutionStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Can only run automation in active (IN_PROGRESS) test plans");
        }

        if (caseIds == null || caseIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No cases specified for batch execution");
        }

        if (caseIds.size() > MAX_BATCH_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Batch limit exceeded: maximum allowed is " + MAX_BATCH_SIZE + " cases per batch");
        }

        if (caseIds.stream().anyMatch(Objects::isNull)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Case IDs cannot be null");
        String type = batchType == null || batchType.isBlank() ? "SELECTED" : batchType.toUpperCase(Locale.ROOT);
        if (!Set.of("SELECTED", "ALL_ELIGIBLE", "RETRY_FAILED").contains(type))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown batch type");
        Set<Integer> uniqueIds = new LinkedHashSet<>(caseIds);
        if (uniqueIds.size() != caseIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate case IDs specified in batch request");
        }

        // Validate and load all links under lock
        List<TestCaseExecutions> linksToQueue = new ArrayList<>();
        for (Integer caseId : uniqueIds) {
            TestCaseExecutions link = linkRepository.findByTestExecutionsIdAndTestCaseId(planId, caseId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Case #" + caseId + " is not part of this plan"));

            var snapshot = snapshots.read(link);
            if (snapshot.type() != TestTypes.CUCUMBER_AUTOMATION && snapshot.type() != TestTypes.KEYWORD_DRIVEN) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Case #" + caseId + " is not an automated test case");
            }

            if (jobRepository.existsByCaseExecutionIdAndJobStatusIn(link.getId(), List.of(JobStatus.QUEUED, JobStatus.RUNNING))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "An automation run is already pending for case #" + caseId + ". Cancel it before starting another.");
            }

            policy.requireRecord(link);
            linksToQueue.add(link);
        }

        // Load project locators and variables once for all cases in batch
        var locators = jobService.loadProjectLocators(plan.getProjects().getId());
        var vars = jobService.loadProjectVariablesAndSecrets(plan.getProjects().getId());

        // Create durable batch record
        AutomationBatch batch = new AutomationBatch();
        batch.setPlanId(planId);
        batch.setProjectId(plan.getProjects().getId());
        batch.setBatchType(type);
        batch.setStatus(BatchStatus.QUEUED);
        batch.setTotalCases(linksToQueue.size());
        batch.setCreatedAt(Instant.now());
        batch.setCreatedBy(policy.currentName());
        batch = batchRepository.save(batch);

        // Queue all jobs atomically linking to batchId
        for (TestCaseExecutions link : linksToQueue) {
            jobService.queueJobForLink(plan, link, batch.getId(), locators, vars.variables(), vars.secretKeys());
        }

        return batch;
    }

    @Transactional(readOnly = true)
    public BatchDetailResponse getBatch(Long batchId) {
        AutomationBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Automation batch not found"));

        if (!projectAccess.project(batch.getProjectId())) {
            throw new org.springframework.security.access.AccessDeniedException("Project access required");
        }

        TestExecutions plan = planRepository.findById((int) batch.getPlanId());
        String planName = plan != null ? plan.getExecutionName() : "Plan #" + batch.getPlanId();

        List<AutomationJob> jobs = jobRepository.findByBatchIdOrderByIdAsc(batchId);

        int queued = 0;
        int running = 0;
        int passed = 0;
        int failed = 0;
        int infrastructureError = 0;
        int cancelled = 0;

        List<BatchJobSummary> jobSummaries = new ArrayList<>();

        for (AutomationJob job : jobs) {
            switch (job.getJobStatus()) {
                case QUEUED -> queued++;
                case RUNNING -> running++;
                case COMPLETED -> passed++;
                case FAILED -> failed++;
                case INFRASTRUCTURE_ERROR -> infrastructureError++;
                case CANCELLED -> cancelled++;
            }

            Long durationMs = null;
            if (job.getStartedAt() != null && job.getFinishedAt() != null) {
                durationMs = Math.max(0, job.getFinishedAt().toEpochMilli() - job.getStartedAt().toEpochMilli());
            }

            String stepSummary = null;
            if (job.getStepResultsJson() != null && !job.getStepResultsJson().isBlank()) {
                try {
                    List<AutomationExecutionResult.StepResult> steps = mapper.readValue(
                            job.getStepResultsJson(),
                            new TypeReference<List<AutomationExecutionResult.StepResult>>() {}
                    );
                    long passedSteps = steps.stream().filter(AutomationExecutionResult.StepResult::isPassed).count();
                    stepSummary = passedSteps + "/" + steps.size() + " steps passed";
                } catch (Exception ignored) {}
            }

            String caseName = "Case #" + job.getCaseId();
            try {
                caseName = mapper.readValue(job.getTestDefinition(), AutomationTestDefinition.class).getCaseName();
            } catch (Exception ignored) { /* Older records may lack a readable definition. */ }
            if (caseName == null || caseName.isBlank()) caseName = "Case #" + job.getCaseId();

            jobSummaries.add(BatchJobSummary.builder()
                    .jobId(job.getId())
                    .caseId(job.getCaseId())
                    .caseName(caseName)
                    .testType(job.getTestType())
                    .status(job.getJobStatus())
                    .durationMs(durationMs)
                    .errorMessage(job.getErrorMessage())
                    .stepSummary(stepSummary)
                    .startedAt(job.getStartedAt())
                    .finishedAt(job.getFinishedAt())
                    .assignedWorkerId(job.getAssignedWorkerId())
                    .canCancel(unfinished(job) && policy.canRecord(job.getCaseExecution()))
                    .build());
        }

        // Determine dynamic batch status
        BatchStatus computedStatus;
        if (queued > 0 || running > 0) {
            computedStatus = (running > 0 || passed > 0 || failed > 0 || infrastructureError > 0 || cancelled > 0)
                    ? BatchStatus.RUNNING : BatchStatus.QUEUED;
        } else {
            if (failed > 0 || infrastructureError > 0) {
                computedStatus = BatchStatus.FAILED;
            } else if (cancelled > 0) {
                computedStatus = BatchStatus.CANCELLED;
            } else if (passed == batch.getTotalCases()) {
                computedStatus = BatchStatus.COMPLETED;
            } else {
                computedStatus = BatchStatus.FAILED;
            }
        }

        // Job rows are the source of truth. Polling must never overwrite cancellation metadata.
        boolean isFinished = queued == 0 && running == 0;
        Instant finishedAt = isFinished ? jobs.stream().map(AutomationJob::getFinishedAt)
                .filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(batch.getFinishedAt()) : null;
        boolean canCancel = jobs.stream().anyMatch(job -> unfinished(job) && policy.canRecord(job.getCaseExecution()));
        boolean canRetry = isFinished && jobs.stream().anyMatch(job -> retryable(job))
                && jobs.stream().filter(this::retryable).allMatch(job -> policy.canRecord(job.getCaseExecution()));

        int resolvedCount = passed + failed + infrastructureError + cancelled;
        int percentComplete = batch.getTotalCases() > 0 ? (resolvedCount * 100) / batch.getTotalCases() : 0;

        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("queued", queued);
        counts.put("running", running);
        counts.put("passed", passed);
        counts.put("failed", failed);
        counts.put("infrastructureError", infrastructureError);
        counts.put("cancelled", cancelled);

        return BatchDetailResponse.builder()
                .batchId(batch.getId())
                .planId(batch.getPlanId())
                .planName(planName)
                .projectId(batch.getProjectId())
                .batchType(batch.getBatchType())
                .status(computedStatus)
                .totalCases(batch.getTotalCases())
                .counts(counts)
                .percentComplete(percentComplete)
                .finished(isFinished)
                .canCancel(canCancel)
                .canRetryFailed(canRetry)
                .createdAt(batch.getCreatedAt())
                .finishedAt(finishedAt)
                .createdBy(batch.getCreatedBy())
                .cancelledAt(batch.getCancelledAt())
                .cancelledBy(batch.getCancelledBy())
                .jobs(jobSummaries)
                .build();
    }

    @Transactional(readOnly = true)
    public Optional<BatchDetailResponse> getLatestBatchForPlan(int planId) {
        TestExecutions plan = planRepository.findById(planId);
        if (plan == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Test plan not found");
        }

        if (!projectAccess.project(plan.getProjects().getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Project access required");
        }

        return batchRepository.findFirstByPlanIdOrderByIdDesc(planId)
                .map(b -> getBatch(b.getId()));
    }

    private boolean unfinished(AutomationJob job) {
        return job.getJobStatus() == JobStatus.QUEUED || job.getJobStatus() == JobStatus.RUNNING;
    }

    private boolean retryable(AutomationJob job) {
        return job.getJobStatus() == JobStatus.FAILED || job.getJobStatus() == JobStatus.INFRASTRUCTURE_ERROR;
    }

    public BatchDetailResponse cancelBatch(Long batchId) {
        AutomationBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Automation batch not found"));
        if (!projectAccess.project(batch.getProjectId()))
            throw new org.springframework.security.access.AccessDeniedException("Project access required");

        // Workers lock a job before its plan. Lock ALL jobs in ID order before acquiring the plan,
        // otherwise a second worker can hold another job while waiting on our plan lock.
        List<AutomationJob> jobs = new ArrayList<>();
        for (Long jobId : jobRepository.findIdsByBatchId(batchId)) {
            AutomationJob job = jobRepository.findForUpdate(jobId).orElseThrow();
            entityManager.refresh(job, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            jobs.add(job);
        }
        TestExecutions plan = planRepository.findForUpdate(batch.getPlanId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
        entityManager.refresh(plan, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (jobs.stream().anyMatch(this::unfinished) && plan.getExecutionStatus() != ExecutionStatus.IN_PROGRESS)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reopen and start this plan before cancelling jobs");

        String cancelUser = policy.currentName();
        Instant now = Instant.now();
        boolean anyCancelled = false;
        for (AutomationJob job : jobs) {
            if (!unfinished(job)) continue;
            entityManager.refresh(job.getCaseExecution(), jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            if (!policy.canRecord(job.getCaseExecution())) continue;
            job.setJobStatus(JobStatus.CANCELLED);
            jobService.clearPreview(job.getId());
            job.setFinishedAt(now);
            job.setLeaseExpiration(null);
            job.setErrorMessage("Cancelled by user " + cancelUser);
            anyCancelled = true;
        }
        if (!anyCancelled && jobs.stream().anyMatch(this::unfinished))
            throw new org.springframework.security.access.AccessDeniedException("No unfinished cases in this batch are assigned to you");
        if (anyCancelled) {
            // This metadata is only written by cancellation, never by polling.
            batch.setCancelledAt(now);
            batch.setCancelledBy(cancelUser);
        }
        return getBatch(batchId);
    }

    public AutomationBatch retryFailed(Long batchId) {
        AutomationBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Automation batch not found"));

        if (!projectAccess.project(batch.getProjectId())) {
            throw new org.springframework.security.access.AccessDeniedException("Project access required");
        }

        TestExecutions plan = planRepository.findForUpdate(batch.getPlanId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));

        if (plan.getExecutionStatus() != ExecutionStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Can only retry automation in active (IN_PROGRESS) test plans");
        }

        List<AutomationJob> jobs = jobRepository.findByBatchIdOrderByIdAsc(batchId);
        if (jobs.stream().anyMatch(this::unfinished))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Wait for this batch to finish before retrying failed cases");
        List<Integer> failedOrCancelledCaseIds = jobs.stream()
                .filter(this::retryable)
                .map(AutomationJob::getCaseId)
                .distinct()
                .toList();

        if (failedOrCancelledCaseIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No failed cases found in batch #" + batchId + " to retry");
        }

        return createBatch(batch.getPlanId(), failedOrCancelledCaseIds, "RETRY_FAILED");
    }
}
