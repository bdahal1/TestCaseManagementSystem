package com.tcms.automation;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.tcms.enums.ExecutionStatus;
import com.tcms.enums.TestResult;
import com.tcms.enums.TestTypes;
import com.tcms.models.*;
import com.tcms.repositories.*;
import com.tcms.security.ProjectAccess;
import com.tcms.services.ExecutionPolicy;
import com.tcms.services.TestCaseSnapshots;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class AutomationJobService {
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    private final AutomationJobRepository jobRepository;
    private final AutomationJobPreviewRepository previewRepository;
    private final TestExecutionRepository planRepository;
    private final TestCaseExecutionRepository linkRepository;
    private final ProjectLocatorRepository locatorRepository;
    private final ProjectVariableRepository variableRepository;
    private final ExecutionEvidenceRepository evidenceRepository;
    private final ProjectAccess projectAccess;
    private final ExecutionPolicy policy;
    private final TestCaseSnapshots snapshots;
    private final AutomationStepSerializer serializer;
    private final AutomationValidator validator;
    private final ObjectMapper mapper;

    public AutomationJobService(AutomationJobRepository jobRepository,
                                AutomationJobPreviewRepository previewRepository,
                                TestExecutionRepository planRepository,
                                TestCaseExecutionRepository linkRepository,
                                ProjectLocatorRepository locatorRepository,
                                ProjectVariableRepository variableRepository,
                                ExecutionEvidenceRepository evidenceRepository,
                                ProjectAccess projectAccess,
                                ExecutionPolicy policy,
                                TestCaseSnapshots snapshots,
                                AutomationStepSerializer serializer,
                                AutomationValidator validator,
                                ObjectMapper mapper) {
        this.jobRepository = jobRepository;
        this.previewRepository = previewRepository;
        this.planRepository = planRepository;
        this.linkRepository = linkRepository;
        this.locatorRepository = locatorRepository;
        this.variableRepository = variableRepository;
        this.evidenceRepository = evidenceRepository;
        this.projectAccess = projectAccess;
        this.policy = policy;
        this.snapshots = snapshots;
        this.serializer = serializer;
        this.validator = validator;
        this.mapper = mapper;
    }

    public Map<String, AutomationTestDefinition.LocatorDefinition> loadProjectLocators(int projectId) {
        Map<String, AutomationTestDefinition.LocatorDefinition> locatorsMap = new HashMap<>();
        List<ProjectLocator> projectLocators = locatorRepository.findByProjectIdOrderByLocatorNameAsc(projectId);
        for (ProjectLocator loc : projectLocators) {
            locatorsMap.put(loc.getLocatorName(), AutomationTestDefinition.LocatorDefinition.builder()
                    .type(loc.getLocatorType())
                    .value(loc.getLocatorValue())
                    .description(loc.getDescription())
                    .build());
        }
        return locatorsMap;
    }

    public record ProjectVariablesAndSecrets(Map<String, String> variables, Set<String> secretKeys) {}

    public ProjectVariablesAndSecrets loadProjectVariablesAndSecrets(int projectId) {
        Map<String, String> variablesMap = new HashMap<>();
        Set<String> secretKeys = new HashSet<>();
        List<ProjectVariable> projectVars = variableRepository.findByProjectIdOrderByVariableKeyAsc(projectId);
        for (ProjectVariable pv : projectVars) {
            variablesMap.put(pv.getVariableKey(), pv.getVariableValue());
            if (Boolean.TRUE.equals(pv.getIsSecret())) {
                secretKeys.add(pv.getVariableKey());
            }
        }
        return new ProjectVariablesAndSecrets(variablesMap, secretKeys);
    }

    public AutomationJob queueJobForLink(TestExecutions plan,
                                         TestCaseExecutions link,
                                         Long batchId,
                                         Map<String, AutomationTestDefinition.LocatorDefinition> locatorsMap,
                                         Map<String, String> variablesMap,
                                         Set<String> secretKeys) {
        if (!Objects.equals(plan.getId(), link.getTestExecutions().getId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Case does not belong to this plan");
        if (!projectAccess.project(plan.getProjects().getId()))
            throw new org.springframework.security.access.AccessDeniedException("Project access required");
        entityManager.refresh(link, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        policy.requireRecord(link);
        if (jobRepository.existsByCaseExecutionIdAndJobStatusIn(link.getId(), List.of(JobStatus.QUEUED, JobStatus.RUNNING)))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An automation run is already pending for this case. Cancel it before starting another.");

        snapshots.capture(link);
        var snapshot = snapshots.read(link);
        if (snapshot.type() != TestTypes.CUCUMBER_AUTOMATION && snapshot.type() != TestTypes.KEYWORD_DRIVEN)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only Cucumber Automation and Keyword Driven cases can be run");
        List<AutomationStepModel> steps = readAutomationSteps(snapshot);

        // Validate steps & locators before queuing
        var validation = validator.validate(steps, locatorsMap);
        if (!validation.isValid()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Automation configuration error: " + redactConfigurationError(validation.getJoinedMessage(), variablesMap, secretKeys));
        }

        AutomationTestDefinition definition = AutomationTestDefinition.builder()
                .caseId(link.getTestCase().getId())
                .caseName(snapshot.name())
                .testType(snapshot.type())
                .steps(steps)
                .locators(locatorsMap)
                .variables(variablesMap)
                .secretKeys(secretKeys)
                .executionConfig(Map.of("stepTimeoutMs", "8000", "headless", "true"))
                .build();

        String definitionJson;
        try {
            definitionJson = mapper.writeValueAsString(definition);
        } catch (JacksonException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to serialize test definition", e);
        }

        AutomationJob job = new AutomationJob();
        job.setCaseExecution(link);
        job.setPlanId(plan.getId());
        job.setCaseId(link.getTestCase().getId());
        job.setProjectId(plan.getProjects().getId());
        job.setBatchId(batchId);
        job.setCaseName(snapshot.name());
        job.setJobStatus(JobStatus.QUEUED);
        job.setTestType(snapshot.type());
        job.setTestDefinition(definitionJson);
        job.setExpectedResultRevision(revision(link));
        job.setCreatedAt(Instant.now());
        job.setCreatedBy(policy.currentName());
        return jobRepository.save(job);
    }

    public List<AutomationStepModel> readAutomationSteps(com.tcms.dto.plans.PlanResponses.Snapshot snapshot) {
        List<AutomationStepModel> steps = new ArrayList<>();
        if (snapshot.steps() != null) for (var saved : snapshot.steps()) {
            AutomationStepModel step = serializer.deserializeStep(saved.data());
            if (step == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Step " + saved.order() + " has an invalid automation definition");
            steps.add(step);
        }
        return steps;
    }

    public String redactConfigurationError(String message, Map<String, String> variables, Set<String> secretKeys) {
        return GherkinStrings.redact(message, secretKeys.stream().map(variables::get).filter(Objects::nonNull).toList());
    }

    public AutomationJob createRunJob(int planId, int caseId) {
        TestExecutions plan = planRepository.findForUpdate(planId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Test plan not found"));
        entityManager.refresh(plan, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (!projectAccess.project(plan.getProjects().getId()))
            throw new org.springframework.security.access.AccessDeniedException("Project access required");
        if (plan.getExecutionStatus() != ExecutionStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Can only run automation in active (IN_PROGRESS) test plans");
        }

        TestCaseExecutions link = linkRepository.findByTestExecutionsIdAndTestCaseId(planId, caseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case is not part of this plan"));

        var locators = loadProjectLocators(plan.getProjects().getId());
        var vars = loadProjectVariablesAndSecrets(plan.getProjects().getId());

        return queueJobForLink(plan, link, null, locators, vars.variables(), vars.secretKeys());
    }

    public record LeasedJob(AutomationJob job, String leaseToken) {}

    private static boolean finished(AutomationJob job) {
        return Set.of(JobStatus.COMPLETED, JobStatus.FAILED, JobStatus.INFRASTRUCTURE_ERROR, JobStatus.CANCELLED).contains(job.getJobStatus());
    }
    private static long revision(TestCaseExecutions link) { return link.getResultRevision()==null ? 0 : link.getResultRevision(); }
    private ResponseStatusException conflict(String reason) { return new ResponseStatusException(HttpStatus.CONFLICT,reason); }
    private String hash(String token) {
        try { return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private void workerId(String workerId) {
        if (workerId==null || !workerId.matches("[A-Za-z0-9._-]{1,120}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid worker ID");
    }
    private AutomationJob lockWorkerJob(Long id, int projectId, String workerId, String leaseToken) {
        workerId(workerId);
        AutomationJob job=jobRepository.findForUpdate(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Job not found"));
        if (!Objects.equals(job.getProjectId(),projectId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Job not found");
        if (!Objects.equals(job.getAssignedWorkerId(),workerId) || leaseToken==null || job.getLeaseTokenHash()==null
                || !java.security.MessageDigest.isEqual(hash(leaseToken).getBytes(java.nio.charset.StandardCharsets.US_ASCII),job.getLeaseTokenHash().getBytes(java.nio.charset.StandardCharsets.US_ASCII)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"This worker does not hold the job lease");
        return job;
    }
    private TestExecutions lockPlan(AutomationJob job) {
        TestExecutions plan = planRepository.findForUpdate(job.getPlanId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Plan not found"));
        // The job's eager relationships may have been loaded before the plan lock was acquired.
        entityManager.refresh(plan, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        entityManager.refresh(job.getCaseExecution(), jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        return plan;
    }
    private String obsoleteReason(AutomationJob job, TestExecutions plan) {
        if (plan.getExecutionStatus()!=ExecutionStatus.IN_PROGRESS) return "The plan is no longer active";
        if (job.getExpectedResultRevision()==null || revision(job.getCaseExecution())!=job.getExpectedResultRevision()) return "The case result changed after this job was queued";
        var latest=jobRepository.findFirstByCaseExecutionIdOrderByIdDesc(job.getCaseExecution().getId());
        if (latest.isEmpty() || !latest.get().getId().equals(job.getId())) return "A newer automation run superseded this job";
        return null;
    }
    private void cancel(AutomationJob job,String reason) {
        clearPreview(job.getId());
        job.setJobStatus(JobStatus.CANCELLED); job.setFinishedAt(Instant.now()); job.setLeaseExpiration(null); job.setErrorMessage(reason);
    }
    private void requireCurrentLease(AutomationJob job, TestExecutions plan) {
        if (job.getJobStatus()!=JobStatus.RUNNING) throw conflict("Job is not running; discard this attempt");
        if (job.getLeaseExpiration()==null || !job.getLeaseExpiration().isAfter(Instant.now())) throw conflict("Job lease expired; discard this attempt");
        String reason=obsoleteReason(job,plan);
        if (reason!=null) { cancel(job,reason); throw conflict(reason+"; late results discarded"); }
    }

    public Optional<LeasedJob> pollNextJob(int projectId, String workerId) {
        workerId(workerId);
        // Claims and callbacks lock a job before its plan. Plan-only operations never acquire job locks.
        for (int skipped=0; skipped<25; skipped++) {
            Instant now=Instant.now();
            List<AutomationJob> eligible=jobRepository.findNextEligibleJobs(projectId,now,PageRequest.of(0,1));
            if (eligible.isEmpty()) return Optional.empty();
            AutomationJob job=eligible.getFirst();
            if (finished(job) || job.getJobStatus()==JobStatus.RUNNING && job.getLeaseExpiration()!=null && job.getLeaseExpiration().isAfter(now)) return Optional.empty();
            String reason=obsoleteReason(job,lockPlan(job));
            if (reason!=null) { cancel(job,reason); jobRepository.flush(); continue; }
            byte[] random=new byte[32]; new java.security.SecureRandom().nextBytes(random);
            String token=Base64.getUrlEncoder().withoutPadding().encodeToString(random);
            job.setJobStatus(JobStatus.RUNNING); job.setAssignedWorkerId(workerId); job.setLeaseTokenHash(hash(token));
            if (job.getStartedAt()==null) job.setStartedAt(now);
            job.setLeaseExpiration(now.plusSeconds(90));
            clearPreview(job.getId());
            return Optional.of(new LeasedJob(job,token));
        }
        return Optional.empty();
    }

    @Transactional(noRollbackFor=ResponseStatusException.class)
    public void heartbeat(Long jobId, int projectId, String workerId, String leaseToken) {
        AutomationJob job=lockWorkerJob(jobId,projectId,workerId,leaseToken);
        requireCurrentLease(job,lockPlan(job));
        job.setLeaseExpiration(Instant.now().plusSeconds(90));
    }

    public void clearPreview(Long jobId) { previewRepository.deleteById(jobId); }

    @Transactional(noRollbackFor=ResponseStatusException.class)
    public void updateProgress(Long jobId, int projectId, String workerId, String leaseToken, AutomationProgress progress) {
        AutomationJob job = lockWorkerJob(jobId, projectId, workerId, leaseToken);
        requireCurrentLease(job, lockPlan(job));
        var definition = mapper.readValue(job.getTestDefinition(), AutomationTestDefinition.class);
        int count = definition.getSteps().size();
        if (progress == null || progress.sequence() < 1 || progress.currentStepIndex() < 0 || progress.currentStepIndex() >= count
                || !Set.of("RUNNING", "PASSED", "FAILED").contains(progress.stepStatus() == null ? "" : progress.stepStatus())
                || progress.completedSteps() == null || progress.completedSteps().size() > count
                || progress.completedSteps().stream().anyMatch(Objects::isNull)
                || progress.screenshot() != null && (progress.screenshot().length > 512 * 1024 || !isJpeg(progress.screenshot())))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid automation progress");
        int expectedCompleted = progress.currentStepIndex() + ("RUNNING".equals(progress.stepStatus()) ? 0 : 1);
        if (progress.completedSteps().size() != expectedCompleted)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Progress does not match the current step");
        var preview = previewRepository.findById(jobId).orElseGet(AutomationJobPreview::new);
        if (progress.sequence() <= preview.getSequence()) return; // Ignore delayed/duplicate frames.
        AutomationBrowserDriver redactor = new AutomationBrowserDriver(definition);
        List<Map<String, Object>> steps = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            var step = definition.getSteps().get(index);
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("stepOrder", step.getOrder());
            view.put("description", redactor.redact(definition.getTestType() == TestTypes.CUCUMBER_AUTOMATION
                    ? serializer.toGherkinStep(step) : serializer.toKeywordDescription(step)));
            view.put("keyword", step.getKeyword());
            String status = index == progress.currentStepIndex() ? progress.stepStatus() : "PENDING";
            if (index < progress.completedSteps().size()) {
                var completed = progress.completedSteps().get(index);
                status = completed.isPassed() ? "PASSED" : "FAILED";
                view.put("durationMs", Math.max(0, completed.getDurationMs()));
                String error = redactor.redact(completed.getErrorMessage());
                view.put("errorMessage", error == null ? null : error.substring(0, Math.min(error.length(), 4000)));
            }
            view.put("status", status);
            steps.add(view);
        }
        preview.setJobId(jobId); preview.setSequence(progress.sequence());
        // Viewer revision stays increasing across a reclaimed job whose worker sequence starts at one.
        long previousRevision = preview.getUpdatedAt() == null ? 0 : preview.getUpdatedAt().toEpochMilli();
        preview.setUpdatedAt(Instant.ofEpochMilli(Math.max(System.currentTimeMillis(), previousRevision + 1)));
        preview.setProgressJson(mapper.writeValueAsString(Map.of("currentStepIndex", progress.currentStepIndex(), "steps", steps)));
        if (progress.screenshot() != null) preview.setScreenshot(progress.screenshot());
        previewRepository.save(preview);
    }

    private boolean isJpeg(byte[] image) {
        return image.length >= 4 && image[0] == (byte) 0xff && image[1] == (byte) 0xd8
                && image[image.length - 2] == (byte) 0xff && image[image.length - 1] == (byte) 0xd9;
    }

    @Transactional(readOnly=true)
    public Map<String, Object> getPreview(Long jobId, long afterSequence) {
        AutomationJob job = getJob(jobId); // Uses the same project authorization as results and evidence.
        if (job.getJobStatus() == JobStatus.QUEUED || job.getJobStatus() == JobStatus.CANCELLED) return null;
        if (job.getJobStatus() == JobStatus.RUNNING && (job.getLeaseExpiration() == null || !job.getLeaseExpiration().isAfter(Instant.now()))) return null;
        var preview = previewRepository.findById(jobId).orElse(null);
        if (preview == null || preview.getUpdatedAt().isBefore(Instant.now().minusSeconds(900)) || preview.getUpdatedAt().toEpochMilli() <= afterSequence) return null;
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("jobId", jobId); view.put("sequence", preview.getUpdatedAt().toEpochMilli()); view.put("updatedAt", preview.getUpdatedAt());
        view.put("progress", mapper.readTree(preview.getProgressJson()));
        view.put("screenshot", preview.getScreenshot());
        return view;
    }

    @Transactional(noRollbackFor=ResponseStatusException.class)
    public void uploadEvidence(Long jobId, int projectId, String workerId, String leaseToken, MultipartFile file) {
        AutomationJob job=lockWorkerJob(jobId,projectId,workerId,leaseToken);
        requireCurrentLease(job,lockPlan(job));
        if (file.isEmpty() || file.getSize()>5*1024*1024) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Evidence must be between 1 byte and 5 MB");
        try {
            storeEvidence(job,file.getOriginalFilename(),file.getContentType(),file.getBytes());
        } catch (IOException e) { throw new IllegalStateException("Failed to store evidence",e); }
    }
    private ExecutionEvidence storeEvidence(AutomationJob job,String filename,String contentType,byte[] content) {
        if (evidenceRepository.countByCaseExecutionId(job.getCaseExecution().getId())>=10)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"At most 10 evidence files are allowed per case");
        ExecutionEvidence evidence=new ExecutionEvidence(); evidence.setCaseExecution(job.getCaseExecution());
        String name=filename==null ? "automation-evidence.png" : filename.replace('\\','/'); name=name.substring(name.lastIndexOf('/')+1);
        evidence.setName(name.length()>150 ? name.substring(0,150) : name);
        evidence.setContentType(contentType==null ? "application/octet-stream" : contentType);
        evidence.setSize(content.length); evidence.setContent(content); evidence.setUploadedAt(Instant.now()); evidence.setUploadedBy("Worker: "+job.getAssignedWorkerId());
        return evidenceRepository.save(evidence);
    }

    @Transactional(noRollbackFor=ResponseStatusException.class)
    public void completeJob(Long jobId, int projectId, String workerId, String leaseToken, AutomationExecutionResult result) {
        AutomationJob job=lockWorkerJob(jobId,projectId,workerId,leaseToken);
        // A repeated callback for this exact claim is harmless, even after a newer run has started.
        if (finished(job) && job.getJobStatus()!=JobStatus.CANCELLED) return;
        requireCurrentLease(job,lockPlan(job));
        if (result==null || result.getStatus()==null || !Set.of(JobStatus.COMPLETED,JobStatus.FAILED,JobStatus.INFRASTRUCTURE_ERROR).contains(result.getStatus())
                || result.getDurationMs()<0 || result.getStepResults()==null || result.getStepResults().size()>1000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid terminal automation result");
        AutomationTestDefinition definition=mapper.readValue(job.getTestDefinition(),AutomationTestDefinition.class);
        if (result.getStatus()==JobStatus.COMPLETED && (result.getStepResults().size()!=definition.getSteps().size() || result.getStepResults().stream().anyMatch(step -> step==null || !step.isPassed())))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A passing run must include a passed result for every configured step");
        if (result.getStepResults().stream().anyMatch(Objects::isNull)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Missing step result");
        if (result.getFailureScreenshot()!=null && result.getFailureScreenshot().length>5*1024*1024)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Screenshot exceeds 5 MB");
        // Redaction also runs at ingestion, rather than relying only on worker logging.
        AutomationBrowserDriver redactor=new AutomationBrowserDriver(definition);
        String error=redactor.redact(result.getErrorMessage()); if (error!=null && error.length()>4000) error=error.substring(0,4000);
        result.getStepResults().forEach(step -> { step.setDescription(redactor.redact(step.getDescription())); step.setErrorMessage(redactor.redact(step.getErrorMessage())); });
        Instant now=Instant.now(); TestCaseExecutions link=job.getCaseExecution();
        // Save optional evidence before changing the job or case, so validation failures leave the run retryable.
        if (result.getFailureScreenshot()!=null && result.getFailureScreenshot().length>0) {
            if (evidenceRepository.countByCaseExecutionId(job.getCaseExecution().getId()) < 10) {
                var evidence = storeEvidence(job,result.getFailureScreenshotName(),"image/png",result.getFailureScreenshot());
                job.setFailureEvidenceId(evidence.getId());
                result.getStepResults().stream().filter(step -> !step.isPassed()).findFirst().ifPresent(step -> step.setEvidenceId(evidence.getId()));
            } else {
                String logs = result.getDiagnosticLogs() == null ? "" : result.getDiagnosticLogs();
                result.setDiagnosticLogs(logs + "\nScreenshot omitted: this case already has 10 evidence files. Existing evidence was preserved.");
            }
        }
        job.setJobStatus(result.getStatus()); job.setFinishedAt(now); job.setLeaseExpiration(null);
        job.setErrorMessage(error); job.setDiagnosticLogs(redactor.redact(result.getDiagnosticLogs())); job.setStepResultsJson(mapper.writeValueAsString(result.getStepResults()));
        TestResult outcome=switch(result.getStatus()) { case COMPLETED -> TestResult.PASS; case FAILED -> TestResult.FAIL; default -> TestResult.BLOCKED; };
        String summary=switch(result.getStatus()) {
            case COMPLETED -> "Automated run passed ("+result.getDurationMs()+"ms, "+result.getStepResults().size()+" steps)";
            case FAILED -> "Automated run failed: "+(error==null ? "Assertion error" : error);
            default -> "Infrastructure failure: "+(error==null ? "Browser launch/environment error" : error);
        };
        link.setResultStatus(outcome); link.setActualResult(summary); link.setResultComment(error);
        link.setRecordedAt(now); link.setRecordedBy("Automation Worker ("+workerId+")"); link.setResultRevision(revision(link)+1);
        ExecutionResultHistory attempt=new ExecutionResultHistory(); attempt.setCaseExecution(link); attempt.setStatus(outcome);
        attempt.setComment(error); attempt.setActualResult(summary); attempt.setDefectUrl(link.getDefectUrl()); attempt.setRecordedAt(now); attempt.setRecordedBy(link.getRecordedBy()); link.getHistory().add(attempt);
    }

    public void cancelJob(Long jobId) {
        AutomationJob job=jobRepository.findForUpdate(jobId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Job not found"));
        if (!projectAccess.project(job.getProjectId()))
            throw new org.springframework.security.access.AccessDeniedException("Project access required");
        lockPlan(job); policy.requireRecord(job.getCaseExecution());
        if (finished(job)) throw conflict("Cannot cancel an already finished job");
        cancel(job,"Cancelled by user "+policy.currentName());
    }

    public AutomationJob getJob(Long jobId) {
        AutomationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
        if (!projectAccess.project(job.getProjectId())) {
            throw new org.springframework.security.access.AccessDeniedException("Project access required");
        }
        return job;
    }

    public Optional<AutomationJob> getLatestJobForCase(int planId, int caseId) {
        TestCaseExecutions link = linkRepository.findByTestExecutionsIdAndTestCaseId(planId, caseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case execution not found"));
        if (!projectAccess.project(link.getTestExecutions().getProjects().getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Project access required");
        }
        return jobRepository.findFirstByCaseExecutionIdOrderByIdDesc(link.getId());
    }
}
