package com.tcms.automation;

import com.tcms.authentication.config.JwtTokenUtil;
import com.tcms.authentication.service.CustomUserDetailService;
import com.tcms.automation.history.AttemptSummary;
import com.tcms.automation.history.CompareAttemptResponse;
import com.tcms.automation.history.HistoryPageResponse;
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
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
public class AutomationHistoryTest {

    @Autowired
    private MockMvc mvc;

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

    private Projects projectA;
    private Projects projectB;
    private Users userA;
    private Users userB;
    private String tokenA;
    private String tokenB;
    private TestExecutions planA1;
    private TestExecutions planA2;
    private TestCase caseA1;
    private TestCase caseA2;
    private TestCaseExecutions linkA1;
    private TestCaseExecutions linkA2;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> seedFixture());
    }

    private void seedFixture() {
        Roles engineerRole = roleRepository.findByRoleNameContaining("ROLE_QAEngineer", PageRequest.of(0, 1))
                .stream().findFirst().orElseGet(() -> {
                    Roles r = new Roles();
                    r.setRoleName("ROLE_QAEngineer");
                    return roleRepository.save(r);
                });

        projectA = new Projects();
        projectA.setProjectName("HistoryProjA-" + UUID.randomUUID());
        projectA.setProjectInitials("HPA" + UUID.randomUUID().toString().substring(0, 4));
        projectA = projectRepository.save(projectA);

        projectB = new Projects();
        projectB.setProjectName("HistoryProjB-" + UUID.randomUUID());
        projectB.setProjectInitials("HPB" + UUID.randomUUID().toString().substring(0, 4));
        projectB = projectRepository.save(projectB);

        userA = new Users();
        userA.setUserName("usera-" + UUID.randomUUID());
        userA.setFirstName("User");
        userA.setLastName("A");
        userA.setPassword("password");
        userA.setIsActive(true);
        userA.setRoleSet(new HashSet<>(Set.of(engineerRole)));
        userA.setProjectsSet(new HashSet<>(Set.of(projectA)));
        userA = userRepository.save(userA);

        userB = new Users();
        userB.setUserName("userb-" + UUID.randomUUID());
        userB.setFirstName("User");
        userB.setLastName("B");
        userB.setPassword("password");
        userB.setIsActive(true);
        userB.setRoleSet(new HashSet<>(Set.of(engineerRole)));
        userB.setProjectsSet(new HashSet<>(Set.of(projectB)));
        userB = userRepository.save(userB);

        caseA1 = createTestCase(projectA, TestTypes.CUCUMBER_AUTOMATION, "Login Test");
        caseA2 = createTestCase(projectA, TestTypes.KEYWORD_DRIVEN, "Checkout Flow");

        planA1 = new TestExecutions();
        planA1.setExecutionName("Sprint 1 Automation");
        planA1.setExecutionStatus(ExecutionStatus.IN_PROGRESS);
        planA1.setProjects(projectA);
        planA1 = testPlanRepository.save(planA1);

        planA2 = new TestExecutions();
        planA2.setExecutionName("Sprint 2 Regression");
        planA2.setExecutionStatus(ExecutionStatus.IN_PROGRESS);
        planA2.setProjects(projectA);
        planA2 = testPlanRepository.save(planA2);

        linkA1 = linkCaseToPlan(planA1, caseA1);
        linkA2 = linkCaseToPlan(planA2, caseA2);

        var detailsA = customUserDetailService.loadUserByUsername(userA.getUserName());
        tokenA = tokenUtil.generateToken(detailsA);

        var detailsB = customUserDetailService.loadUserByUsername(userB.getUserName());
        tokenB = tokenUtil.generateToken(detailsB);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userA.getUserName(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_QAEngineer"))));
    }

    private TestCase createTestCase(Projects p, TestTypes type, String name) {
        TestCase c = new TestCase();
        c.setProjects(entityManager.getReference(Projects.class, p.getId()));
        c.setTestName(name);
        c.setTestProjectId(UUID.randomUUID().toString().substring(0, 8));
        c.setTestType(type);
        c.setTestCreatedBy("QA");
        c.setTestModifiedBy("QA");
        c.setTestCreatedDate(new Timestamp(System.currentTimeMillis()));
        c.setTestModifiedDate(c.getTestCreatedDate());
        c.setTestStepsSet(new HashSet<>());
        return testCaseRepository.save(c);
    }

    private TestCaseExecutions linkCaseToPlan(TestExecutions plan, TestCase tc) {
        TestCaseExecutions link = new TestCaseExecutions();
        link.setTestExecutions(plan);
        link.setTestCase(tc);
        link.setResultStatus(TestResult.NOT_RUN);
        return linkRepository.save(link);
    }

    private AutomationJob createJob(Projects p, TestExecutions plan, TestCase tc, TestCaseExecutions link,
                                    JobStatus status, Instant createdAt, String frozenCaseName, String defJson, String resultsJson) {
        AutomationJob j = new AutomationJob();
        j.setProjectId(p.getId());
        j.setPlanId(plan.getId());
        j.setCaseId(tc.getId());
        j.setCaseExecution(link);
        j.setCaseName(frozenCaseName);
        j.setJobStatus(status);
        j.setTestType(tc.getTestType());
        j.setCreatedAt(createdAt);
        j.setStartedAt(createdAt.plusMillis(100));
        j.setFinishedAt(createdAt.plusMillis(1500));
        j.setCreatedBy("Tester");

        if (defJson != null) {
            j.setTestDefinition(defJson);
        } else {
            j.setTestDefinition("{\"caseId\":" + tc.getId() + ",\"caseName\":\"" + frozenCaseName + "\",\"testType\":\"" + tc.getTestType() + "\",\"steps\":[]}");
        }

        j.setStepResultsJson(resultsJson);
        return jobRepository.save(j);
    }

    @Test
    @DisplayName("Pagination and stable ordering by id DESC")
    void testHistoryPaginationAndStableOrdering() throws Exception {
        Instant base = Instant.now().minus(10, ChronoUnit.HOURS);
        for (int i = 1; i <= 25; i++) {
            createJob(projectA, planA1, caseA1, linkA1,
                    i % 2 == 0 ? JobStatus.COMPLETED : JobStatus.FAILED,
                    base.plus(i, ChronoUnit.MINUTES), "Frozen Case " + i, null, null);
        }

        String resPage0 = mvc.perform(get("/automation/history/project/" + projectA.getId() + "?page=0&size=10")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content.length()").value(10))
                .andReturn().getResponse().getContentAsString();

        HistoryPageResponse p0 = objectMapper.readValue(resPage0, HistoryPageResponse.class);
        for (int i = 0; i < p0.getContent().size() - 1; i++) {
            assertThat(p0.getContent().get(i).getJobId())
                    .isGreaterThan(p0.getContent().get(i + 1).getJobId());
        }

        String resPage1 = mvc.perform(get("/automation/history/project/" + projectA.getId() + "?page=1&size=10")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(10))
                .andReturn().getResponse().getContentAsString();

        HistoryPageResponse p1 = objectMapper.readValue(resPage1, HistoryPageResponse.class);
        assertThat(p0.getContent().get(9).getJobId())
                .isGreaterThan(p1.getContent().get(0).getJobId());
    }

    @Test
    @DisplayName("Filtering by plan, status, date range, and frozen case name")
    void testHistoryFiltering() throws Exception {
        Instant t1 = Instant.parse("2026-05-01T10:00:00Z");
        Instant t2 = Instant.parse("2026-05-02T10:00:00Z");
        Instant t3 = Instant.parse("2026-05-03T10:00:00Z");

        createJob(projectA, planA1, caseA1, linkA1, JobStatus.COMPLETED, t1, "User Login Smoke", null, null);
        createJob(projectA, planA1, caseA2, linkA1, JobStatus.FAILED, t2, "Cart Checkout Flow", null, null);
        createJob(projectA, planA2, caseA1, linkA2, JobStatus.COMPLETED, t3, "User Login Regression", null, null);

        // Filter by planId
        mvc.perform(get("/automation/history/project/" + projectA.getId() + "?planId=" + planA1.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        // Filter by status
        mvc.perform(get("/automation/history/project/" + projectA.getId() + "?status=FAILED")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].caseName").value("Cart Checkout Flow"));

        // Filter by caseNameContains
        mvc.perform(get("/automation/history/project/" + projectA.getId() + "?caseNameContains=login")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        // Filter by date range
        mvc.perform(get("/automation/history/project/" + projectA.getId() + "?from=2026-05-01T12:00:00Z&to=2026-05-02T12:00:00Z")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].caseName").value("Cart Checkout Flow"));
    }

    @Test
    @DisplayName("Frozen case name is preserved even if library case is renamed")
    void testFrozenRunNamePreserved() throws Exception {
        AutomationJob job = createJob(projectA, planA1, caseA1, linkA1, JobStatus.COMPLETED,
                Instant.now(), "Original Frozen Name v1", null, null);

        // Rename the test case in library
        caseA1.setTestName("Completely Modified Library Name v2");
        testCaseRepository.save(caseA1);
        entityManager.flush();

        mvc.perform(get("/automation/history/project/" + projectA.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].jobId").value(job.getId()))
                .andExpect(jsonPath("$.content[0].caseName").value("Original Frozen Name v1"));
    }

    @Test
    @DisplayName("Project isolation: User in Project B cannot access Project A history or jobs")
    void testProjectIsolation() throws Exception {
        AutomationJob jobA = createJob(projectA, planA1, caseA1, linkA1, JobStatus.COMPLETED,
                Instant.now(), "Project A Run", null, null);

        // List history of project A as User B -> 403 Forbidden
        mvc.perform(get("/automation/history/project/" + projectA.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // Get job A details as User B using project A in path -> 403 Forbidden
        mvc.perform(get("/automation/history/project/" + projectA.getId() + "/job/" + jobA.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // Attempting to access job A through project B -> 404 Not Found (avoids leaking existence)
        mvc.perform(get("/automation/history/project/" + projectB.getId() + "/job/" + jobA.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Exact-attempt navigation and read-only details")
    void testExactJobDetail() throws Exception {
        String resultsJson = "[{\"stepOrder\":1,\"keyword\":\"Given\",\"description\":\"Given open page \\\"http://test\\\"\",\"passed\":true,\"durationMs\":150}," +
                "{\"stepOrder\":2,\"keyword\":\"When\",\"description\":\"When click element \\\"#btn\\\"\",\"passed\":false,\"durationMs\":300,\"errorMessage\":\"Element not found\"}]";

        AutomationJob job = createJob(projectA, planA1, caseA1, linkA1, JobStatus.FAILED,
                Instant.now(), "Frozen Login", null, resultsJson);
        job.setErrorMessage("Assertion failed: Element not found");
        job.setDiagnosticLogs("Step 1 OK\nStep 2 failed on #btn");
        jobRepository.save(job);

        mvc.perform(get("/automation/history/project/" + projectA.getId() + "/job/" + job.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(job.getId()))
                .andExpect(jsonPath("$.planId").value(planA1.getId()))
                .andExpect(jsonPath("$.planName").value(planA1.getExecutionName()))
                .andExpect(jsonPath("$.caseName").value("Frozen Login"))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.stepResults.length()").value(2))
                .andExpect(jsonPath("$.stepResults[0].passed").value(true))
                .andExpect(jsonPath("$.stepResults[1].passed").value(false))
                .andExpect(jsonPath("$.stepResults[1].errorMessage").value("Element not found"))
                .andExpect(jsonPath("$.diagnosticLogs").value("Step 1 OK\nStep 2 failed on #btn"));
    }

    @Test
    @DisplayName("Attempt list for UI previous-attempt selector")
    void testAttemptList() throws Exception {
        Instant now = Instant.now();
        AutomationJob j1 = createJob(projectA, planA1, caseA1, linkA1, JobStatus.FAILED, now.minusSeconds(100), "Case 1", null, null);
        AutomationJob j2 = createJob(projectA, planA1, caseA1, linkA1, JobStatus.COMPLETED, now.minusSeconds(50), "Case 1", null, null);

        String json = mvc.perform(get("/automation/history/project/" + projectA.getId() + "/case/" + caseA1.getId() + "/plan/" + planA1.getId() + "/attempts")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        List<AttemptSummary> attempts = objectMapper.readValue(json, new TypeReference<List<AttemptSummary>>() {});
        assertThat(attempts.get(0).getJobId()).isEqualTo(j1.getId());
        assertThat(attempts.get(0).getAttemptNumber()).isEqualTo(1);
        assertThat(attempts.get(0).getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(attempts.get(1).getJobId()).isEqualTo(j2.getId());
        assertThat(attempts.get(1).getAttemptNumber()).isEqualTo(2);
        assertThat(attempts.get(1).getStatus()).isEqualTo(JobStatus.COMPLETED);
    }

    @Test
    @DisplayName("Comparison handles changed step definitions explicitly without false alignment")
    void testCompareAttemptsWithChangedSteps() throws Exception {
        // Attempt 1: Steps [Open Page, Click Login]
        String defA = "{\"caseId\":" + caseA1.getId() + ",\"caseName\":\"Login\",\"testType\":\"CUCUMBER_AUTOMATION\",\"steps\":[" +
                "{\"order\":1,\"keyword\":\"Given\",\"template\":\"OPEN_PAGE\",\"textValue\":\"https://example.com/login\"}," +
                "{\"order\":2,\"keyword\":\"When\",\"template\":\"CLICK_ELEMENT\",\"locatorValue\":\"#login-btn\"}" +
                "]}";
        String resA = "[{\"stepOrder\":1,\"keyword\":\"Given\",\"description\":\"Given open page \\\"https://example.com/login\\\"\",\"passed\":true,\"durationMs\":200}," +
                "{\"stepOrder\":2,\"keyword\":\"When\",\"description\":\"When click element \\\"#login-btn\\\"\",\"passed\":false,\"durationMs\":100,\"errorMessage\":\"Failed button\"}]";

        // Attempt 2: Modified step 2, and added step 3!
        // Step 1: Open Page (same)
        // Step 2: Enter text (new)
        // Step 3: Click Login (same action as step 2 of attempt 1, but order is now 3)
        String defB = "{\"caseId\":" + caseA1.getId() + ",\"caseName\":\"Login\",\"testType\":\"CUCUMBER_AUTOMATION\",\"steps\":[" +
                "{\"order\":1,\"keyword\":\"Given\",\"template\":\"OPEN_PAGE\",\"textValue\":\"https://example.com/login\"}," +
                "{\"order\":2,\"keyword\":\"When\",\"template\":\"ENTER_TEXT\",\"locatorValue\":\"#user\",\"textValue\":\"admin\"}," +
                "{\"order\":3,\"keyword\":\"When\",\"template\":\"CLICK_ELEMENT\",\"locatorValue\":\"#login-btn\"}" +
                "]}";
        String resB = "[{\"stepOrder\":1,\"keyword\":\"Given\",\"description\":\"Given open page \\\"https://example.com/login\\\"\",\"passed\":true,\"durationMs\":220}," +
                "{\"stepOrder\":2,\"keyword\":\"When\",\"description\":\"When enter text \\\"admin\\\" into \\\"#user\\\"\",\"passed\":true,\"durationMs\":50}," +
                "{\"stepOrder\":3,\"keyword\":\"When\",\"description\":\"When click element \\\"#login-btn\\\"\",\"passed\":true,\"durationMs\":110}]";

        AutomationJob jobA = createJob(projectA, planA1, caseA1, linkA1, JobStatus.FAILED, Instant.now().minusSeconds(60), "Login", defA, resA);
        AutomationJob jobB = createJob(projectA, planA1, caseA1, linkA1, JobStatus.COMPLETED, Instant.now(), "Login", defB, resB);

        String json = mvc.perform(get("/automation/history/project/" + projectA.getId() + "/compare?jobIdA=" + jobA.getId() + "&jobIdB=" + jobB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        CompareAttemptResponse comp = objectMapper.readValue(json, CompareAttemptResponse.class);
        assertThat(comp.isHasChangedSteps()).isTrue();
        assertThat(comp.getStatusA()).isEqualTo("FAILED");
        assertThat(comp.getStatusB()).isEqualTo("COMPLETED");

        // The step rows should contain 3 entries:
        // 1. Open Page (present in both: passedA=true, passedB=true, onlyInA=false, onlyInB=false)
        // 2. Click Login (present in both despite changing from step 2 in A to step 3 in B: passedA=false, passedB=true)
        // 3. Enter Text (only in B: onlyInB=true, onlyInA=false)
        assertThat(comp.getSteps()).hasSize(3);

        var openPage = comp.getSteps().stream()
                .filter(s -> s.getDescription().contains("open page"))
                .findFirst().orElseThrow();
        assertThat(openPage.getPassedA()).isTrue();
        assertThat(openPage.getPassedB()).isTrue();
        assertThat(openPage.isOnlyInA()).isFalse();
        assertThat(openPage.isOnlyInB()).isFalse();

        var clickBtn = comp.getSteps().stream()
                .filter(s -> s.getDescription().contains("click element"))
                .findFirst().orElseThrow();
        assertThat(clickBtn.getPassedA()).isFalse();
        assertThat(clickBtn.getPassedB()).isTrue();
        assertThat(clickBtn.isOnlyInA()).isFalse();
        assertThat(clickBtn.isOnlyInB()).isFalse();

        var enterText = comp.getSteps().stream()
                .filter(s -> s.getDescription().contains("enter text"))
                .findFirst().orElseThrow();
        assertThat(enterText.isOnlyInB()).isTrue();
        assertThat(enterText.getPassedA()).isNull();
        assertThat(enterText.getPassedB()).isTrue();
    }

    @Test
    @DisplayName("Secrets masking in history detail, compare, and diagnostic logs")
    void testSecretMaskingInHistory() throws Exception {
        String secretVal = "SuperSecretPassword123!";
        ProjectVariable pv = new ProjectVariable();
        pv.setProject(projectA);
        pv.setVariableKey("API_KEY");
        pv.setVariableValue(secretVal);
        pv.setIsSecret(true);
        entityManager.persist(pv);

        String def = "{\"caseId\":" + caseA1.getId() + ",\"caseName\":\"Secret Test\",\"testType\":\"CUCUMBER_AUTOMATION\",\"steps\":[" +
                "{\"order\":1,\"keyword\":\"When\",\"template\":\"ENTER_TEXT\",\"locatorValue\":\"#pwd\",\"textValue\":\"" + secretVal + "\"}" +
                "]}";
        String res = "[{\"stepOrder\":1,\"keyword\":\"When\",\"description\":\"When enter text \\\"" + secretVal + "\\\" into \\\"#pwd\\\"\",\"passed\":false,\"durationMs\":100,\"errorMessage\":\"Failed with secret " + secretVal + "\"}]";

        AutomationJob job = createJob(projectA, planA1, caseA1, linkA1, JobStatus.FAILED, Instant.now(), "Secret Test", def, res);
        job.setErrorMessage("Crash with secret: " + secretVal);
        job.setDiagnosticLogs("Logs mentioning secret: " + secretVal);
        jobRepository.save(job);

        // Check Job Detail
        mvc.perform(get("/automation/history/project/" + projectA.getId() + "/job/" + job.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorMessage").value("Crash with secret: [REDACTED]"))
                .andExpect(jsonPath("$.diagnosticLogs").value("Logs mentioning secret: [REDACTED]"))
                .andExpect(jsonPath("$.stepResults[0].description").value("When enter text \"[REDACTED]\" into \"#pwd\""))
                .andExpect(jsonPath("$.stepResults[0].errorMessage").value("Failed with secret [REDACTED]"));
    }
}
