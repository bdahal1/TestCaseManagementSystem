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

    public AutomationJob createRunJob(int planId, int caseId) {
        TestExecutions plan = planRepository.findForUpdate(planId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Test plan not found"));
        if (!projectAccess.project(plan.getProjects().getId()))
            throw new org.springframework.security.access.AccessDeniedException("Project access required");
        if (plan.getExecutionStatus() != ExecutionStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Can only run automation in active (IN_PROGRESS) test plans");
        }

        TestCaseExecutions link = linkRepository.findByTestExecutionsIdAndTestCaseId(planId, caseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case is not part of this plan"));

        policy.requireRecord(link);
        if (jobRepository.existsByCaseExecutionIdAndJobStatusIn(link.getId(), List.of(JobStatus.QUEUED, JobStatus.RUNNING)))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An automation run is already pending for this case. Cancel it before starting another.");

        TestCase testCase = link.getTestCase();
        if (testCase.getTestType() != TestTypes.CUCUMBER_AUTOMATION && testCase.getTestType() != TestTypes.KEYWORD_DRIVEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only Cucumber Automation and Keyword Driven cases can be run");
        }

        // Ensure snapshot is captured
        snapshots.capture(link);
        var snapshot = snapshots.read(link);

        // Deserializing steps
        List<AutomationStepModel> steps = new ArrayList<>();
        if (snapshot.steps() != null) {
            for (var s : snapshot.steps()) {
                if (s.data() != null && !s.data().isBlank()) {
                    var parsed = serializer.deserializeStep(s.data());
                    if (parsed != null) {
                        steps.add(parsed);
                    }
                }
            }
        }

        // Build locators map
        Map<String, AutomationTestDefinition.LocatorDefinition> locatorsMap = new HashMap<>();
        List<ProjectLocator> projectLocators = locatorRepository.findByProjectIdOrderByLocatorNameAsc(plan.getProjects().getId());
        for (ProjectLocator loc : projectLocators) {
            locatorsMap.put(loc.getLocatorName(), AutomationTestDefinition.LocatorDefinition.builder()
                    .type(loc.getLocatorType())
                    .value(loc.getLocatorValue())
                    .description(loc.getDescription())
                    .build());
        }

        // Validate steps & locators before queuing
        var validation = validator.validate(steps, locatorsMap);
        if (!validation.isValid()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Automation configuration error: " + validation.getJoinedMessage());
        }

        // Build variables and secrets map
        Map<String, String> variablesMap = new HashMap<>();
        Set<String> secretKeys = new HashSet<>();
        List<ProjectVariable> projectVars = variableRepository.findByProjectIdOrderByVariableKeyAsc(plan.getProjects().getId());
        for (ProjectVariable pv : projectVars) {
            variablesMap.put(pv.getVariableKey(), pv.getVariableValue());
            if (Boolean.TRUE.equals(pv.getIsSecret())) {
                secretKeys.add(pv.getVariableKey());
            }
        }

        AutomationTestDefinition definition = AutomationTestDefinition.builder()
                .caseId(caseId)
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
        job.setPlanId(planId);
        job.setCaseId(caseId);
        job.setProjectId(plan.getProjects().getId());
        job.setJobStatus(JobStatus.QUEUED);
        job.setTestType(snapshot.type());
        job.setTestDefinition(definitionJson);
        job.setExpectedResultRevision(revision(link));
        job.setCreatedAt(Instant.now());
        job.setCreatedBy(policy.currentName());
        return jobRepository.save(job);
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

    @Transactional(noRollbackFor=ResponseStatusException.class)
    public void uploadEvidence(Long jobId, int projectId, String workerId, String leaseToken, MultipartFile file) {
        AutomationJob job=lockWorkerJob(jobId,projectId,workerId,leaseToken);
        requireCurrentLease(job,lockPlan(job));
        if (file.isEmpty() || file.getSize()>5*1024*1024) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Evidence must be between 1 byte and 5 MB");
        try {
            storeEvidence(job,file.getOriginalFilename(),file.getContentType(),file.getBytes());
        } catch (IOException e) { throw new IllegalStateException("Failed to store evidence",e); }
    }
    private void storeEvidence(AutomationJob job,String filename,String contentType,byte[] content) {
        if (evidenceRepository.findByCaseExecutionIdOrderByUploadedAtAsc(job.getCaseExecution().getId()).size()>=10)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"At most 10 evidence files are allowed per case");
        ExecutionEvidence evidence=new ExecutionEvidence(); evidence.setCaseExecution(job.getCaseExecution());
        String name=filename==null ? "automation-evidence.png" : filename.replace('\\','/'); name=name.substring(name.lastIndexOf('/')+1);
        evidence.setName(name.length()>255 ? name.substring(0,255) : name);
        evidence.setContentType(contentType==null ? "application/octet-stream" : contentType);
        evidence.setSize(content.length); evidence.setContent(content); evidence.setUploadedAt(Instant.now()); evidence.setUploadedBy("Worker: "+job.getAssignedWorkerId());
        evidenceRepository.save(evidence);
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
        if (result.getFailureScreenshot()!=null && result.getFailureScreenshot().length>0)
            storeEvidence(job,result.getFailureScreenshotName(),"image/png",result.getFailureScreenshot());
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
