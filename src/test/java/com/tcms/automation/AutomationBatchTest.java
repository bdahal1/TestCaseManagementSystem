package com.tcms.automation;

import com.tcms.authentication.config.JwtTokenUtil;
import com.tcms.authentication.service.CustomUserDetailService;
import com.tcms.automation.batch.*;
import com.tcms.enums.ExecutionStatus;
import com.tcms.enums.TestResult;
import com.tcms.enums.TestTypes;
import com.tcms.models.*;
import com.tcms.repositories.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
public class AutomationBatchTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AutomationBatchService batchService;

    @Autowired
    private AutomationBatchRepository batchRepository;

    @Autowired
    private AutomationJobRepository jobRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private TestExecutionRepository testPlanRepository;

    @Autowired
    private TestCaseExecutionRepository linkRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JwtTokenUtil tokenUtil;

    @Autowired
    private CustomUserDetailService customUserDetailService;

    private Projects project;
    private Users manager;
    private String managerToken;
    private TestExecutions activePlan;
    private TestCase cucumberCase;
    private TestCase keywordCase;
    private TestCase manualCase;
    private TestCaseExecutions cucumberLink;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> seedFixture());
    }

    private void seedFixture() {
        project = new Projects();
        project.setProjectName("BatchProj-" + UUID.randomUUID());
        project.setProjectInitials(UUID.randomUUID().toString().substring(0, 8));
        project = projectRepository.save(project);

        Roles managerRole = roleRepository.findByRoleNameContaining("ROLE_QAManager", PageRequest.of(0, 1))
                .stream().findFirst().orElseGet(() -> {
                    Roles r = new Roles();
                    r.setRoleName("ROLE_QAManager");
                    return roleRepository.save(r);
                });

        manager = new Users();
        manager.setUserName("batchmgr-" + UUID.randomUUID());
        manager.setFirstName("QA");
        manager.setLastName("Manager");
        manager.setPassword("password");
        manager.setRoleSet(new HashSet<>(Set.of(managerRole)));
        manager.setProjectsSet(new HashSet<>(Set.of(project)));
        manager = userRepository.save(manager);

        cucumberCase = createTestCase(project, TestTypes.CUCUMBER_AUTOMATION,
                "{\"order\":1,\"keyword\":\"Given\",\"template\":\"OPEN_PAGE\",\"textValue\":\"https://example.com\"}");

        keywordCase = createTestCase(project, TestTypes.KEYWORD_DRIVEN,
                "{\"order\":1,\"template\":\"OPEN_PAGE\",\"textValue\":\"https://example.com/search\"}");

        manualCase = createTestCase(project, TestTypes.MANUAL, null);

        activePlan = new TestExecutions();
        activePlan.setExecutionName("Sprint 1 Automation Plan");
        activePlan.setExecutionStatus(ExecutionStatus.IN_PROGRESS);
        activePlan.setProjects(project);
        activePlan = testPlanRepository.save(activePlan);

        cucumberLink = linkCaseToPlan(activePlan, cucumberCase);
        linkCaseToPlan(activePlan, keywordCase);
        linkCaseToPlan(activePlan, manualCase);

        var userDetails = customUserDetailService.loadUserByUsername(manager.getUserName());
        managerToken = tokenUtil.generateToken(userDetails);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(manager.getUserName(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_QAManager"))));
    }

    private TestCase createTestCase(Projects p, TestTypes type, String stepJson) {
        TestCase c = new TestCase();
        c.setProjects(entityManager.getReference(Projects.class, p.getId()));
        c.setTestName("Case-" + UUID.randomUUID());
        c.setTestProjectId(UUID.randomUUID().toString().substring(0, 8));
        c.setTestType(type);
        c.setTestCreatedBy("QA");
        c.setTestModifiedBy("QA");
        c.setTestCreatedDate(new Timestamp(System.currentTimeMillis()));
        c.setTestModifiedDate(c.getTestCreatedDate());

        if (stepJson != null) {
            TestSteps step = new TestSteps();
            step.setTestStepOrder(1);
            step.setTestType(type);
            step.setTestStepDesc("Execute automation step");
            step.setTestStepData(stepJson);
            step.setTestExpectedOutput("Expected result");
            step.setTsCreatedBy("QA");
            step.setTsModifiedBy("QA");
            step.setTsCreatedDate(c.getTestCreatedDate());
            step.setTsModifiedDate(c.getTestCreatedDate());
            step.setTestCase(c);
            c.setTestStepsSet(new HashSet<>(Set.of(step)));
        } else {
            c.setTestStepsSet(new HashSet<>());
        }

        return testCaseRepository.save(c);
    }

    private TestCaseExecutions linkCaseToPlan(TestExecutions plan, TestCase tc) {
        TestCaseExecutions link = new TestCaseExecutions();
        link.setTestExecutions(plan);
        link.setTestCase(tc);
        link.setResultStatus(TestResult.NOT_RUN);
        link.setAssignee(manager);
        return linkRepository.save(link);
    }

    @Test
    @DisplayName("Preview identifies ready automated cases and skips manual cases")
    void previewBatch_mixedPlan_skipsManual() throws Exception {
        BatchPreviewRequest req = new BatchPreviewRequest();
        req.setScope("ALL_ELIGIBLE");

        mvc.perform(post("/automation/batches/plan/" + activePlan.getId() + "/preview")
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").value(activePlan.getId()))
                .andExpect(jsonPath("$.canExecute").value(true))
                .andExpect(jsonPath("$.readyCount").value(2))
                .andExpect(jsonPath("$.manualSkippedCases.length()").value(1))
                .andExpect(jsonPath("$.manualSkippedCases[0].caseId").value(manualCase.getId()))
                .andExpect(jsonPath("$.readyCases.length()").value(2));
    }

    @Test
    @DisplayName("Preview excludes cases with invalid step configurations")
    void previewBatch_invalidSteps_excluded() throws Exception {
        TestCaseExecutions link = linkRepository.findByTestExecutionsIdAndTestCaseId(
                activePlan.getId(), keywordCase.getId()).orElseThrow();
        link.setCaseSnapshot("{\"name\":\"Broken\",\"type\":\"KEYWORD_DRIVEN\",\"steps\":[]}");
        linkRepository.save(link);

        BatchPreviewRequest req = new BatchPreviewRequest();
        req.setScope("ALL_ELIGIBLE");

        mvc.perform(post("/automation/batches/plan/" + activePlan.getId() + "/preview")
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readyCount").value(1))
                .andExpect(jsonPath("$.invalidConfigCases.length()").value(1))
                .andExpect(jsonPath("$.invalidConfigCases[0].caseId").value(keywordCase.getId()));
    }

    @Test
    @DisplayName("Preview excludes cases already queued or running")
    void previewBatch_alreadyPending_excluded() throws Exception {
        AutomationJob existingJob = new AutomationJob();
        existingJob.setPlanId(activePlan.getId());
        existingJob.setCaseId(cucumberCase.getId());
        existingJob.setCaseExecution(cucumberLink);
        existingJob.setProjectId(project.getId());
        existingJob.setTestType(cucumberCase.getTestType());
        existingJob.setJobStatus(JobStatus.QUEUED);
        existingJob.setTestDefinition("{}");
        existingJob.setCreatedAt(Instant.now());
        jobRepository.save(existingJob);

        BatchPreviewRequest req = new BatchPreviewRequest();
        req.setScope("ALL_ELIGIBLE");

        mvc.perform(post("/automation/batches/plan/" + activePlan.getId() + "/preview")
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readyCount").value(1))
                .andExpect(jsonPath("$.alreadyPendingCases.length()").value(1))
                .andExpect(jsonPath("$.alreadyPendingCases[0].caseId").value(cucumberCase.getId()));
    }

    @Test
    @DisplayName("Create batch atomically queues jobs linked to batch record")
    void createBatch_allEligible_queuesJobs() throws Exception {
        BatchCreateRequest req = new BatchCreateRequest();
        req.setBatchType("ALL_ELIGIBLE");
        req.setCaseIds(List.of(cucumberCase.getId(), keywordCase.getId()));

        String response = mvc.perform(post("/automation/batches/plan/" + activePlan.getId() + "/create")
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.totalCases").value(2))
                .andExpect(jsonPath("$.counts.queued").value(2))
                .andExpect(jsonPath("$.jobs.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        BatchDetailResponse batch = objectMapper.readValue(response, BatchDetailResponse.class);
        assertThat(batch.getBatchId()).isNotNull();

        List<AutomationJob> jobs = jobRepository.findByBatchIdOrderByIdAsc(batch.getBatchId());
        assertThat(jobs).hasSize(2);
        assertThat(jobs).allMatch(j -> j.getBatchId().equals(batch.getBatchId()));
        assertThat(jobs).allMatch(j -> j.getJobStatus() == JobStatus.QUEUED);
    }

    @Test
    @DisplayName("Batch creation on inactive plan is rejected")
    void createBatch_draftPlan_fails() throws Exception {
        activePlan.setExecutionStatus(ExecutionStatus.DRAFT);
        testPlanRepository.save(activePlan);

        BatchCreateRequest req = new BatchCreateRequest();
        req.setBatchType("ALL_ELIGIBLE");
        req.setCaseIds(List.of(cucumberCase.getId()));

        mvc.perform(post("/automation/batches/plan/" + activePlan.getId() + "/create")
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Dynamic status calculation: completed batch with failures is never all-passed")
    void batchStatus_withFailures_isFailed() {
        AutomationBatch created = batchService.createBatch(activePlan.getId(),
                List.of(cucumberCase.getId(), keywordCase.getId()), "ALL_ELIGIBLE");

        List<AutomationJob> jobs = jobRepository.findByBatchIdOrderByIdAsc(created.getId());
        assertThat(jobs).hasSize(2);

        // Job 1 passes
        AutomationJob j1 = jobs.get(0);
        j1.setJobStatus(JobStatus.COMPLETED);
        j1.setFinishedAt(Instant.now());
        jobRepository.save(j1);

        // Job 2 fails
        AutomationJob j2 = jobs.get(1);
        j2.setJobStatus(JobStatus.FAILED);
        j2.setErrorMessage("Assertion failed");
        j2.setFinishedAt(Instant.now());
        jobRepository.save(j2);

        BatchDetailResponse detail = batchService.getBatch(created.getId());
        assertThat(detail.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(detail.isFinished()).isTrue();
        assertThat(detail.getCounts().get("passed")).isEqualTo(1);
        assertThat(detail.getCounts().get("failed")).isEqualTo(1);
    }

    @Test
    @DisplayName("Cancel batch cancels unfinished jobs and preserves completed jobs")
    void cancelBatch_cancelsUnfinishedJobs() {
        AutomationBatch created = batchService.createBatch(activePlan.getId(),
                List.of(cucumberCase.getId(), keywordCase.getId()), "ALL_ELIGIBLE");

        List<AutomationJob> jobs = jobRepository.findByBatchIdOrderByIdAsc(created.getId());
        // Finish job 1 before cancel
        AutomationJob j1 = jobs.get(0);
        j1.setJobStatus(JobStatus.COMPLETED);
        j1.setFinishedAt(Instant.now());
        jobRepository.save(j1);

        BatchDetailResponse cancelled = batchService.cancelBatch(created.getId());
        assertThat(cancelled.getStatus()).isEqualTo(BatchStatus.CANCELLED);
        assertThat(cancelled.getCounts().get("passed")).isEqualTo(1);
        assertThat(cancelled.getCounts().get("cancelled")).isEqualTo(1);

        List<AutomationJob> updatedJobs = jobRepository.findByBatchIdOrderByIdAsc(created.getId());
        assertThat(updatedJobs.get(0).getJobStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(updatedJobs.get(1).getJobStatus()).isEqualTo(JobStatus.CANCELLED);
    }

    @Test
    @DisplayName("Run failed cases again creates a new batch preserving original attempts")
    void retryFailed_createsNewBatch_preservesOriginal() {
        AutomationBatch original = batchService.createBatch(activePlan.getId(),
                List.of(cucumberCase.getId(), keywordCase.getId()), "ALL_ELIGIBLE");

        List<AutomationJob> jobs = jobRepository.findByBatchIdOrderByIdAsc(original.getId());
        // Job 1 passes
        jobs.get(0).setJobStatus(JobStatus.COMPLETED);
        jobRepository.save(jobs.get(0));

        // Job 2 fails
        jobs.get(1).setJobStatus(JobStatus.FAILED);
        jobs.get(1).setErrorMessage("Timed out");
        jobRepository.save(jobs.get(1));

        AutomationBatch retried = batchService.retryFailed(original.getId());
        assertThat(retried.getId()).isNotEqualTo(original.getId());
        assertThat(retried.getTotalCases()).isEqualTo(1);
        assertThat(retried.getStatus()).isEqualTo(BatchStatus.QUEUED);

        // Verify original batch is preserved
        BatchDetailResponse originalAfter = batchService.getBatch(original.getId());
        assertThat(originalAfter.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(originalAfter.getTotalCases()).isEqualTo(2);

        // Verify new batch contains only the failed case
        List<AutomationJob> newJobs = jobRepository.findByBatchIdOrderByIdAsc(retried.getId());
        assertThat(newJobs).hasSize(1);
        assertThat(newJobs.get(0).getCaseId()).isEqualTo(keywordCase.getId());
    }

    @Test
    @DisplayName("Get latest batch for plan returns newest batch")
    void getLatestBatchForPlan_returnsNewest() throws Exception {
        AutomationBatch b1 = batchService.createBatch(activePlan.getId(),
                List.of(cucumberCase.getId()), "ALL_ELIGIBLE");

        // Cancel b1 to allow b2
        batchService.cancelBatch(b1.getId());

        AutomationBatch b2 = batchService.createBatch(activePlan.getId(),
                List.of(cucumberCase.getId()), "ALL_ELIGIBLE");

        mvc.perform(get("/automation/batches/plan/" + activePlan.getId() + "/latest")
                .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchId").value(b2.getId()));
    }
    @Autowired private AutomationJobService jobService;
    @Autowired private org.springframework.core.env.ConfigurableEnvironment environment;
    @org.springframework.boot.test.web.server.LocalServerPort private int port;

    @org.junit.jupiter.api.AfterEach void clearContext() {
        SecurityContextHolder.clearContext();
        environment.getPropertySources().remove("batch-worker-regression");
    }

    private void asManager() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(manager.getUserName(),null,
                List.of(new SimpleGrantedAuthority("ROLE_QAManager"))));
    }

    private Users tester(String name) {
        Roles role=roleRepository.findByRoleNameContaining("ROLE_QAEngineer",PageRequest.of(0,1)).stream().findFirst().orElseGet(()->{
            Roles created=new Roles(); created.setRoleName("ROLE_QAEngineer"); return roleRepository.save(created);
        });
        Users user=new Users(); user.setUserName(name+UUID.randomUUID()); user.setFirstName(name); user.setLastName("QA"); user.setPassword("test-only");
        user.setRoleSet(new HashSet<>(Set.of(role))); user.setProjectsSet(new HashSet<>(Set.of(project)));
        return userRepository.save(user);
    }

    @Test void cancellationHonorsAssignmentsAndClosedPlans() throws Exception {
        activePlan.setReleasePlan(true);
        var owner=tester("Owner"); var other=tester("Other");
        cucumberLink.setAssignee(owner);
        var keywordLink=linkRepository.findByTestExecutionsIdAndTestCaseId(activePlan.getId(),keywordCase.getId()).orElseThrow();
        keywordLink.setAssignee(other);
        var batch=batchService.createBatch(activePlan.getId(),List.of(cucumberCase.getId(),keywordCase.getId()),"SELECTED");
        var outsiderInProject=tester("Reader");
        String token=tokenUtil.generateToken(customUserDetailService.loadUserByUsername(outsiderInProject.getUserName()));
        mvc.perform(post("/automation/batches/"+batch.getId()+"/cancel").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
        assertThat(jobRepository.findByBatchIdOrderByIdAsc(batch.getId())).allMatch(job -> job.getJobStatus()==JobStatus.QUEUED);
        token=tokenUtil.generateToken(customUserDetailService.loadUserByUsername(owner.getUserName()));
        mvc.perform(post("/automation/batches/"+batch.getId()+"/cancel").header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.counts.cancelled").value(1)).andExpect(jsonPath("$.counts.queued").value(1));
        assertThat(jobRepository.findByBatchIdOrderByIdAsc(batch.getId()).stream().filter(j -> j.getCaseId().equals(keywordCase.getId())).findFirst().orElseThrow().getJobStatus()).isEqualTo(JobStatus.QUEUED);
        asManager(); activePlan.setExecutionStatus(ExecutionStatus.CLOSED); entityManager.flush();
        mvc.perform(post("/automation/batches/"+batch.getId()+"/cancel").header("Authorization","Bearer "+managerToken)).andExpect(status().isConflict());
        assertThat(jobRepository.findByBatchIdOrderByIdAsc(batch.getId()).stream().filter(j -> j.getCaseId().equals(keywordCase.getId())).findFirst().orElseThrow().getJobStatus()).isEqualTo(JobStatus.QUEUED);
    }

    @Test void previewDoesNotCaptureDefinitionsAndSavedTypesStayFrozen() {
        assertThat(cucumberLink.getCaseSnapshot()).isNull();
        batchService.previewBatch(activePlan.getId(),null,"ALL_ELIGIBLE");
        assertThat(cucumberLink.getCaseSnapshot()).isNull();
        var saved=new com.tcms.services.TestCaseSnapshots(objectMapper); saved.capture(cucumberLink);
        String originalName=cucumberCase.getTestName();
        cucumberCase.setTestName("Changed library name"); cucumberCase.setTestType(TestTypes.MANUAL); entityManager.flush();
        var preview=batchService.previewBatch(activePlan.getId(),List.of(cucumberCase.getId()),"SELECTED");
        assertThat(preview.getReadyCases()).hasSize(1); assertThat(preview.getReadyCases().getFirst().getCaseName()).isEqualTo(originalName);
        var batch=batchService.createBatch(activePlan.getId(),List.of(cucumberCase.getId()),"SELECTED");
        var job=jobRepository.findByBatchIdOrderByIdAsc(batch.getId()).getFirst();
        assertThat(job.getTestType()).isEqualTo(TestTypes.CUCUMBER_AUTOMATION);
        assertThat(batchService.getBatch(batch.getId()).getJobs().getFirst().getCaseName()).isEqualTo(originalName);
    }

    @Test void invalidStepsAreExcludedFromPreview() {
        var invalid=createTestCase(project,TestTypes.KEYWORD_DRIVEN,"not-json"); linkCaseToPlan(activePlan,invalid);
        var preview=batchService.previewBatch(activePlan.getId(),List.of(invalid.getId()),"SELECTED");
        assertThat(preview.isCanExecute()).isFalse(); assertThat(preview.getInvalidConfigCases()).hasSize(1);

    }

    @Test void previewLimitsAndScopesMatchCreationRules() {
        assertThatThrownBy(()->batchService.previewBatch(activePlan.getId(),null,"TYPO")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(()->batchService.previewBatch(activePlan.getId(),List.of(cucumberCase.getId(),cucumberCase.getId()),"SELECTED"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        for (int i=0;i<99;i++) linkCaseToPlan(activePlan,createTestCase(project,TestTypes.KEYWORD_DRIVEN,
                "{\"order\":1,\"template\":\"OPEN_PAGE\",\"textValue\":\"https://example.test\"}"));
        var preview=batchService.previewBatch(activePlan.getId(),null,"ALL_ELIGIBLE");
        assertThat(preview.getReadyCount()).isEqualTo(101); assertThat(preview.getMaxBatchSize()).isEqualTo(100);
        assertThat(preview.isCanExecute()).isFalse();
    }

    @Test void failedRetryDoesNotIncludeCancelledCasesAndPollingKeepsMetadata() {
        var batch=batchService.createBatch(activePlan.getId(),List.of(cucumberCase.getId(),keywordCase.getId()),"SELECTED");
        var jobs=jobRepository.findByBatchIdOrderByIdAsc(batch.getId());
        jobs.get(0).setJobStatus(JobStatus.FAILED); jobs.get(0).setFinishedAt(Instant.parse("2026-01-01T01:00:00Z"));
        jobs.get(1).setJobStatus(JobStatus.CANCELLED); jobs.get(1).setFinishedAt(Instant.parse("2026-01-01T02:00:00Z"));
        batch.setCancelledAt(Instant.parse("2026-01-01T01:30:00Z")); batch.setCancelledBy("Original canceller"); entityManager.flush();
        var detail=batchService.getBatch(batch.getId());
        assertThat(detail.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(detail.getFinishedAt()).isEqualTo(jobs.get(1).getFinishedAt());
        assertThat(detail.getCancelledBy()).isEqualTo("Original canceller");
        var retry=batchService.retryFailed(batch.getId());
        assertThat(jobRepository.findByBatchIdOrderByIdAsc(retry.getId())).extracting(AutomationJob::getCaseId).containsExactly(cucumberCase.getId());
        assertThat(batch.getCancelledBy()).isEqualTo("Original canceller");
    }

    private AutomationExecutionResult pass() {
        return AutomationExecutionResult.builder().status(JobStatus.COMPLETED).stepResults(List.of(
                AutomationExecutionResult.StepResult.builder().stepOrder(1).passed(true).build())).build();
    }

    @Test
    @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    @org.springframework.test.annotation.DirtiesContext(methodMode=org.springframework.test.annotation.DirtiesContext.MethodMode.AFTER_METHOD)
    void concurrentSubmissionsCancellationAndRealWorkerResultsRemainConsistent() throws Exception {
        var transactions=new TransactionTemplate(transactionManager);
        Integer invalidId=transactions.execute(status -> {
            var invalid=createTestCase(project,TestTypes.KEYWORD_DRIVEN,"not-json");linkCaseToPlan(activePlan,invalid);return invalid.getId();
        });
        assertThatThrownBy(()->batchService.createBatch(activePlan.getId(),List.of(cucumberCase.getId(),invalidId),"SELECTED"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThat(batchRepository.findByPlanIdOrderByIdDesc(activePlan.getId())).isEmpty();
        assertThat(jobRepository.findFirstByCaseExecutionIdOrderByIdDesc(cucumberLink.getId())).isEmpty();
        var start=new java.util.concurrent.CountDownLatch(1);
        var created=new java.util.concurrent.atomic.AtomicReference<Long>();
        try (var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Integer> submit=()->{
                start.await(); asManager();
                try {
                    var batch=batchService.createBatch(activePlan.getId(),List.of(cucumberCase.getId(),keywordCase.getId()),"SELECTED");
                    created.set(batch.getId()); return 201;
                } catch (org.springframework.web.server.ResponseStatusException error) { return error.getStatusCode().value(); }
                finally { SecurityContextHolder.clearContext(); }
            };
            var a=pool.submit(submit); var b=pool.submit(submit); start.countDown();
            assertThat(List.of(a.get(20,java.util.concurrent.TimeUnit.SECONDS),b.get(20,java.util.concurrent.TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201,409);
        }
        assertThat(batchRepository.findByPlanIdOrderByIdDesc(activePlan.getId())).hasSize(1);
        var first=jobService.pollNextJob(project.getId(),"race-a").orElseThrow();
        var second=jobService.pollNextJob(project.getId(),"race-b").orElseThrow();
        var race=new java.util.concurrent.CountDownLatch(1);
        try (var pool=java.util.concurrent.Executors.newFixedThreadPool(3)) {
            var a=pool.submit(()->{race.await(); return completeRace(first,"race-a");});
            var b=pool.submit(()->{race.await(); return completeRace(second,"race-b");});
            var cancel=pool.submit(()->{race.await();asManager();try {return batchService.cancelBatch(created.get());}finally {SecurityContextHolder.clearContext();}});
            race.countDown();
            assertThat(a.get(20,java.util.concurrent.TimeUnit.SECONDS)).isIn(200,409);
            assertThat(b.get(20,java.util.concurrent.TimeUnit.SECONDS)).isIn(200,409);
            cancel.get(20,java.util.concurrent.TimeUnit.SECONDS);
        }
        transactions.executeWithoutResult(status -> {
            var jobs=jobRepository.findByBatchIdOrderByIdAsc(created.get());
            assertThat(jobs).allMatch(job -> job.getJobStatus()==JobStatus.COMPLETED || job.getJobStatus()==JobStatus.CANCELLED);
            for (var job:jobs) assertThat(job.getCaseExecution().getHistory()).hasSize(job.getJobStatus()==JobStatus.COMPLETED ? 1 : 0);
        });

        String local="http://localhost:"+port+"/demo/login";
        var actual=transactions.execute(status -> {
            var plan=new TestExecutions();plan.setExecutionName("Controlled batch");plan.setExecutionStatus(ExecutionStatus.IN_PROGRESS);
            plan.setProjects(entityManager.getReference(Projects.class,project.getId()));testPlanRepository.save(plan);
            var good=createTestCase(project,TestTypes.CUCUMBER_AUTOMATION,
                    "{\"order\":1,\"keyword\":\"Given\",\"template\":\"OPEN_PAGE\",\"textValue\":\""+local+"\"}");
            var bad=createTestCase(project,TestTypes.KEYWORD_DRIVEN,
                    "{\"order\":1,\"template\":\"OPEN_PAGE\",\"textValue\":\""+local+"\"}");
            var extra=new TestSteps();extra.setTestCase(bad);extra.setTestType(TestTypes.KEYWORD_DRIVEN);extra.setTestStepOrder(2);
            extra.setTestStepDesc("Wrong expectation");extra.setTestExpectedOutput("");extra.setTestStepData("{\"order\":2,\"template\":\"VERIFY_TEXT_EQUALS\",\"locatorValue\":\"#login-btn\",\"textValue\":\"Wrong\"}");
            extra.setTsCreatedDate(bad.getTestCreatedDate());extra.setTsModifiedDate(bad.getTestCreatedDate());extra.setTsCreatedBy("QA");extra.setTsModifiedBy("QA");bad.getTestStepsSet().add(extra);testCaseRepository.save(bad);
            linkCaseToPlan(plan,good);linkCaseToPlan(plan,bad);
            var batch=batchService.createBatch(plan.getId(),List.of(good.getId(),bad.getId()),"SELECTED");
            for (var job:jobRepository.findByBatchIdOrderByIdAsc(batch.getId())) {
                var definition=objectMapper.readValue(job.getTestDefinition(),AutomationTestDefinition.class);
                definition.setExecutionConfig(Map.of("stepTimeoutMs","3000","headless","true"));job.setTestDefinition(objectMapper.writeValueAsString(definition));
            }
            return batch;
        });
        String token="batch-regression-worker-token-with-at-least-32-characters";
        environment.getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("batch-worker-regression",Map.of(
                "automation.worker.token",token,"automation.worker.project-id",project.getId())));
        var worker=new com.tcms.automation.worker.AutomationWorkerMain("http://localhost:"+port,token,"batch-worker",true,50);
        worker.run();
        var completed=batchService.getBatch(actual.getId());
        assertThat(completed.isFinished()).isTrue(); assertThat(completed.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(completed.getCounts()).containsEntry("passed",1).containsEntry("failed",1); assertThat(completed.getPercentComplete()).isEqualTo(100);
        var failed=completed.getJobs().stream().filter(job -> job.getStatus()==JobStatus.FAILED).findFirst().orElseThrow();
        Integer evidence=transactions.execute(status -> jobRepository.findById(failed.getJobId()).orElseThrow().getFailureEvidenceId());
        assertThat(evidence).isNotNull();
        mvc.perform(get("/testPlans/"+actual.getPlanId()+"/evidence/"+evidence).header("Authorization","Bearer "+managerToken)).andExpect(status().isOk());
        asManager();
        transactions.executeWithoutResult(status -> {
            var library=testCaseRepository.findById(failed.getCaseId().intValue());library.setTestName("Changed library name");library.setTestType(TestTypes.MANUAL);
        });
        var retryStart=new java.util.concurrent.CountDownLatch(1);
        var accepted=new java.util.concurrent.atomic.AtomicReference<AutomationBatch>();
        try (var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Integer> retryCall=()-> {
                retryStart.await();asManager();
                try {accepted.set(batchService.retryFailed(actual.getId()));return 201;}
                catch(org.springframework.web.server.ResponseStatusException error) {return error.getStatusCode().value();}
                finally {SecurityContextHolder.clearContext();}
            };
            var a=pool.submit(retryCall);var b=pool.submit(retryCall);retryStart.countDown();
            assertThat(List.of(a.get(20,java.util.concurrent.TimeUnit.SECONDS),b.get(20,java.util.concurrent.TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201,409);
        }
        var retry=accepted.get();
        assertThat(retry.getId()).isNotEqualTo(actual.getId());
        assertThat(batchService.getBatch(retry.getId()).getJobs()).hasSize(1).allMatch(job -> job.getCaseName().equals(failed.getCaseName()));
        assertThat(batchService.getBatch(actual.getId()).getCounts()).containsEntry("failed",1);
    }

    private int completeRace(AutomationJobService.LeasedJob claim,String worker) {
        try {jobService.completeJob(claim.job().getId(),project.getId(),worker,claim.leaseToken(),pass());return 200;}
        catch(org.springframework.web.server.ResponseStatusException error) {return error.getStatusCode().value();}
    }

    @Test
    @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    @org.springframework.test.annotation.DirtiesContext(methodMode=org.springframework.test.annotation.DirtiesContext.MethodMode.AFTER_METHOD)
    void fullEvidenceStorageDoesNotPreventRecordingFailure() {
        var transactions=new TransactionTemplate(transactionManager);
        Long batchId=transactions.execute(status -> {
            var batch=batchService.createBatch(activePlan.getId(),List.of(cucumberCase.getId()),"SELECTED");
            var link=linkRepository.findById(cucumberLink.getId()).orElseThrow();
            for (int i=0;i<10;i++) {
                var file=new ExecutionEvidence();file.setCaseExecution(link);file.setName("existing-"+i+".png");file.setContentType("image/png");
                file.setSize(1);file.setContent(new byte[]{1});file.setUploadedAt(Instant.now());file.setUploadedBy("QA");entityManager.persist(file);
            }
            return batch.getId();
        });
        // Claims and results use separate transactions, as they do in actual worker HTTP requests.
        var claim=jobService.pollNextJob(project.getId(),"evidence-worker").orElseThrow();
        var result=AutomationExecutionResult.builder().status(JobStatus.FAILED).failureScreenshot(new byte[]{1,2,3})
                .stepResults(List.of(AutomationExecutionResult.StepResult.builder().stepOrder(1).passed(false).build())).build();
        jobService.completeJob(claim.job().getId(),project.getId(),"evidence-worker",claim.leaseToken(),result);
        transactions.executeWithoutResult(status -> {
            var completed=jobRepository.findById(claim.job().getId()).orElseThrow();
            assertThat(completed.getJobStatus()).isEqualTo(JobStatus.FAILED);
            assertThat(completed.getDiagnosticLogs()).contains("Screenshot omitted");
            assertThat(completed.getFailureEvidenceId()).isNull();
            assertThat(completed.getCaseExecution().getHistory()).hasSize(1);
            assertThat(completed.getCaseExecution().getEvidence()).hasSize(10);
        });
        assertThat(batchService.getBatch(batchId).isFinished()).isTrue();
    }

    @Test void batchValidationErrorsDoNotRevealProjectSecrets() {
        String secret="unsupported-private-key-987";
        var variable=new ProjectVariable();variable.setProject(project);variable.setVariableKey("PASSWORD");variable.setVariableValue(secret);variable.setIsSecret(true);entityManager.persist(variable);
        var invalid=createTestCase(project,TestTypes.KEYWORD_DRIVEN,
                "{\"order\":1,\"template\":\"PRESS_KEY\",\"locatorValue\":\"#input\",\"keyValue\":\""+secret+"\"}");
        linkCaseToPlan(activePlan,invalid);
        var preview=batchService.previewBatch(activePlan.getId(),List.of(invalid.getId()),"SELECTED");
        assertThat(preview.getInvalidConfigCases()).hasSize(1);
        assertThat(preview.getInvalidConfigCases().getFirst().getReason()).contains("[REDACTED]").doesNotContain(secret);
    }

}
