package com.tcms.automation;

import com.tcms.authentication.config.JwtTokenUtil;
import com.tcms.authentication.service.CustomUserDetailService;
import com.tcms.enums.ExecutionStatus;
import com.tcms.enums.TestResult;
import com.tcms.enums.TestTypes;
import com.tcms.models.*;
import com.tcms.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
public class AutomationVerificationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AutomationCucumberRunner cucumberRunner;

    @Autowired
    private AutomationKeywordRunner keywordRunner;

    @Autowired
    private AutomationJobService jobService;

    @Autowired
    private AutomationJobRepository jobRepository;

    @Autowired
    private AutomationProjectExportService exportService;

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
    private ProjectLocatorRepository locatorRepository;

    @Autowired
    private ProjectVariableRepository variableRepository;

    @Autowired
    private JwtTokenUtil tokenUtil;

    @Autowired
    private CustomUserDetailService customUserDetailService;

    private Projects project;
    private Projects foreignProject;
    private Users manager;
    private Users outsider;
    private String managerToken;
    private String outsiderToken;

    @BeforeEach
    void setUp() {
        new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(status -> seedFixture());
    }

    private void seedFixture() {
        project = new Projects();
        project.setProjectName("AutoProj-" + UUID.randomUUID());
        project.setProjectInitials(UUID.randomUUID().toString().substring(0, 8));
        project = projectRepository.save(project);

        foreignProject = new Projects();
        foreignProject.setProjectName("Foreign-" + UUID.randomUUID());
        foreignProject.setProjectInitials(UUID.randomUUID().toString().substring(0, 8));
        foreignProject = projectRepository.save(foreignProject);

        Roles managerRole = new Roles();
        managerRole.setRoleName("ROLE_QAManager");
        managerRole = roleRepository.save(managerRole);

        Roles engineerRole = new Roles();
        engineerRole.setRoleName("ROLE_QAEngineer");
        engineerRole = roleRepository.save(engineerRole);

        manager = new Users();
        manager.setUserName("mgr-" + UUID.randomUUID());
        manager.setFirstName("QA");
        manager.setLastName("Manager");
        manager.setPassword("password");
        manager.setRoleSet(new HashSet<>(Set.of(managerRole)));
        manager.setProjectsSet(new HashSet<>(Set.of(project)));
        manager = userRepository.save(manager);

        outsider = new Users();
        outsider.setUserName("outsider-" + UUID.randomUUID());
        outsider.setFirstName("Other");
        outsider.setLastName("Person");
        outsider.setPassword("password");
        outsider.setRoleSet(new HashSet<>(Set.of(engineerRole)));
        outsider.setProjectsSet(new HashSet<>(Set.of(foreignProject)));
        outsider = userRepository.save(outsider);

        managerToken = "Bearer " + tokenUtil.generateToken(customUserDetailService.loadUserByUsername(manager.getUserName()));
        outsiderToken = "Bearer " + tokenUtil.generateToken(customUserDetailService.loadUserByUsername(outsider.getUserName()));
    }

    private TestCase createTestCase(Projects p, TestTypes type, String stepJson) {
        TestCase c = new TestCase();
        c.setProjects(entityManager.getReference(Projects.class,p.getId()));
        c.setTestName("Case-" + UUID.randomUUID());
        c.setTestProjectId(UUID.randomUUID().toString().substring(0, 8));
        c.setTestType(type);
        c.setTestCreatedBy("QA");
        c.setTestModifiedBy("QA");
        c.setTestCreatedDate(new Timestamp(System.currentTimeMillis()));
        c.setTestModifiedDate(c.getTestCreatedDate());

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
        return testCaseRepository.save(c);
    }

    private List<AutomationStepModel> buildLoginSteps(String baseUrl, String expectedWelcome) {
        List<AutomationStepModel> steps = new ArrayList<>();
        steps.add(AutomationStepModel.builder()
                .order(1)
                .keyword("Given")
                .template(AutomationTemplate.OPEN_PAGE)
                .textValue(baseUrl + "/demo/login")
                .build());
        steps.add(AutomationStepModel.builder()
                .order(2)
                .keyword("When")
                .template(AutomationTemplate.ENTER_TEXT)
                .locatorValue("Login.Username")
                .textValue("admin")
                .build());
        steps.add(AutomationStepModel.builder()
                .order(3)
                .keyword("And")
                .template(AutomationTemplate.ENTER_TEXT)
                .locatorValue("Login.Password")
                .textValue("secret")
                .build());
        steps.add(AutomationStepModel.builder()
                .order(4)
                .keyword("And")
                .template(AutomationTemplate.CLICK_ELEMENT)
                .locatorValue("Login.Submit")
                .build());
        steps.add(AutomationStepModel.builder()
                .order(5)
                .keyword("Then")
                .template(AutomationTemplate.VERIFY_TEXT_EQUALS)
                .locatorValue("#welcome")
                .textValue(expectedWelcome)
                .build());
        steps.add(AutomationStepModel.builder()
                .order(6)
                .keyword("And")
                .template(AutomationTemplate.VERIFY_ELEMENT_STATE)
                .locatorValue("#welcome")
                .stateValue(ElementState.VISIBLE)
                .build());
        return steps;
    }

    private Map<String, AutomationTestDefinition.LocatorDefinition> buildLocators() {
        Map<String, AutomationTestDefinition.LocatorDefinition> map = new HashMap<>();
        map.put("Login.Username", new AutomationTestDefinition.LocatorDefinition(LocatorType.CSS, "#username", "Username field"));
        map.put("Login.Password", new AutomationTestDefinition.LocatorDefinition(LocatorType.CSS, "#password", "Password field"));
        map.put("Login.Submit", new AutomationTestDefinition.LocatorDefinition(LocatorType.CSS, "#login-btn", "Sign in button"));
        return map;
    }

    @Test
    @DisplayName("Verify successful login with Cucumber Automation against demo app")
    void testCucumberAutomationSuccess() {
        String baseUrl = "http://localhost:" + port;
        List<AutomationStepModel> steps = buildLoginSteps(baseUrl, "Welcome, admin!");
        Map<String, AutomationTestDefinition.LocatorDefinition> locators = buildLocators();

        AutomationTestDefinition def = AutomationTestDefinition.builder()
                .caseId(101)
                .caseName("Successful Login Scenario")
                .testType(TestTypes.CUCUMBER_AUTOMATION)
                .steps(steps)
                .locators(locators)
                .executionConfig(Map.of("stepTimeoutMs", "3000", "headless", "true"))
                .build();

        AutomationExecutionResult result = cucumberRunner.execute(def);

        assertThat(result.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(result.getStepResults()).hasSize(6);
        assertThat(result.getStepResults()).allMatch(AutomationExecutionResult.StepResult::isPassed);
        assertThat(result.getFailureScreenshot()).isNull();
    }

    @Test
    @DisplayName("Verify authoring mode equivalence: same login scenario succeeds with Keyword Driven")
    void testKeywordDrivenEquivalence() {
        String baseUrl = "http://localhost:" + port;
        List<AutomationStepModel> steps = buildLoginSteps(baseUrl, "Welcome, admin!");
        Map<String, AutomationTestDefinition.LocatorDefinition> locators = buildLocators();

        AutomationTestDefinition def = AutomationTestDefinition.builder()
                .caseId(102)
                .caseName("Keyword Login Scenario")
                .testType(TestTypes.KEYWORD_DRIVEN)
                .steps(steps)
                .locators(locators)
                .executionConfig(Map.of("stepTimeoutMs", "3000", "headless", "true"))
                .build();

        AutomationExecutionResult result = keywordRunner.execute(def);

        assertThat(result.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(result.getStepResults()).hasSize(6);
        assertThat(result.getStepResults()).allMatch(AutomationExecutionResult.StepResult::isPassed);
        assertThat(result.getFailureScreenshot()).isNull();
    }

    @Test
    @DisplayName("Verify assertion failure produces genuine failure and captures failure screenshot")
    void testAssertionFailureProducesScreenshot() {
        String baseUrl = "http://localhost:" + port;
        // Intentionally wrong expectation to trigger assertion failure
        List<AutomationStepModel> steps = buildLoginSteps(baseUrl, "Welcome, wrong_user!");
        Map<String, AutomationTestDefinition.LocatorDefinition> locators = buildLocators();

        AutomationTestDefinition def = AutomationTestDefinition.builder()
                .caseId(103)
                .caseName("Failing Login Scenario")
                .testType(TestTypes.KEYWORD_DRIVEN)
                .steps(steps)
                .locators(locators)
                .executionConfig(Map.of("stepTimeoutMs", "1000", "headless", "true"))
                .build();

        AutomationExecutionResult result = keywordRunner.execute(def);

        assertThat(result.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(result.getStepResults()).hasSize(5); // Fails on step 5
        assertThat(result.getStepResults().get(4).isPassed()).isFalse();
        assertThat(result.getFailureScreenshot()).isNotNull();
        assertThat(result.getFailureScreenshot().length).isGreaterThan(100);
    }

    @Test
    @DisplayName("Verify missing locator causes step failure and diagnostic log capture")
    void testMissingLocatorFailure() {
        String baseUrl = "http://localhost:" + port;
        List<AutomationStepModel> steps = new ArrayList<>();
        steps.add(AutomationStepModel.builder()
                .order(1)
                .template(AutomationTemplate.OPEN_PAGE)
                .textValue(baseUrl + "/demo/login")
                .build());
        steps.add(AutomationStepModel.builder()
                .order(2)
                .template(AutomationTemplate.CLICK_ELEMENT)
                .locatorValue("NonExistent.Button")
                .build());

        // Do not register NonExistent.Button in locators
        AutomationTestDefinition def = AutomationTestDefinition.builder()
                .caseId(104)
                .caseName("Missing Locator Scenario")
                .testType(TestTypes.KEYWORD_DRIVEN)
                .steps(steps)
                .locators(Collections.emptyMap())
                .executionConfig(Map.of("stepTimeoutMs", "500", "headless", "true"))
                .build();

        AutomationExecutionResult result = keywordRunner.execute(def);

        assertThat(result.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(result.getStepResults().get(1).isPassed()).isFalse();
        assertThat(result.getStepResults().get(1).getErrorMessage()).contains("NonExistent.Button");
    }

    @Test
    @DisplayName("Verify variable interpolation and secret redaction in diagnostic logs")
    void testVariableInterpolationAndSecretMasking() {
        String baseUrl = "http://localhost:" + port;
        List<AutomationStepModel> steps = new ArrayList<>();
        steps.add(AutomationStepModel.builder()
                .order(1)
                .template(AutomationTemplate.OPEN_PAGE)
                .textValue("${appUrl}")
                .build());
        steps.add(AutomationStepModel.builder()
                .order(2)
                .template(AutomationTemplate.ENTER_TEXT)
                .locatorValue("#username")
                .textValue("${user}")
                .build());
        steps.add(AutomationStepModel.builder()
                .order(3)
                .template(AutomationTemplate.ENTER_TEXT)
                .locatorValue("#password")
                .textValue("${super_secret_pw}")
                .build());

        Map<String, String> variables = new HashMap<>();
        variables.put("appUrl", baseUrl + "/demo/login");
        variables.put("user", "admin");
        variables.put("super_secret_pw", "secret");

        AutomationTestDefinition def = AutomationTestDefinition.builder()
                .caseId(105)
                .caseName("Secrets Masking Scenario")
                .testType(TestTypes.KEYWORD_DRIVEN)
                .steps(steps)
                .variables(variables)
                .secretKeys(Set.of("super_secret_pw"))
                .executionConfig(Map.of("stepTimeoutMs", "2000", "headless", "true"))
                .build();

        AutomationExecutionResult result = keywordRunner.execute(def);

        assertThat(result.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(result.getDiagnosticLogs()).doesNotContain("secret");
        assertThat(result.getDiagnosticLogs()).contains("[REDACTED]");
    }

    @Test
    @DisplayName("Verify run snapshot immutability: editing library case does not affect queued/executed job")
    void testRunSnapshotImmutability() {
        // Create library test case with initial step
        TestCase tc = createTestCase(project, TestTypes.CUCUMBER_AUTOMATION,
                "{\"order\":1,\"keyword\":\"Given\",\"template\":\"OPEN_PAGE\",\"textValue\":\"http://localhost/original\"}");

        // Create active test plan
        TestExecutions plan = new TestExecutions();
        plan.setExecutionName("Sprint Plan " + UUID.randomUUID());
        plan.setExecutionStatus(ExecutionStatus.IN_PROGRESS);
        plan.setProjects(project);
        plan = testPlanRepository.save(plan);

        // Add case to plan
        TestCaseExecutions link = new TestCaseExecutions();
        link.setTestExecutions(plan);
        link.setTestCase(tc);
        link.setResultStatus(TestResult.NOT_RUN);
        link = linkRepository.save(link);

        // Authenticate as manager
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        manager.getUserName(), null, List.of(new SimpleGrantedAuthority("ROLE_QAManager"))
                )
        );

        // Schedule automation job
        AutomationJob job = jobService.createRunJob(plan.getId(), tc.getId());
        assertThat(job).isNotNull();
        assertThat(job.getTestDefinition()).contains("http://localhost/original");

        // Now modify the library test case to have completely different steps
        TestSteps step = tc.getTestStepsSet().iterator().next();
        step.setTestStepData("{\"order\":1,\"keyword\":\"Given\",\"template\":\"OPEN_PAGE\",\"textValue\":\"http://localhost/MODIFIED_PATH\"}");
        testCaseRepository.save(tc);

        // Re-read job and verify frozen snapshot remains unchanged
        AutomationJob reloadedJob = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(reloadedJob.getTestDefinition()).contains("http://localhost/original");
        assertThat(reloadedJob.getTestDefinition()).doesNotContain("MODIFIED_PATH");

        // Verify case execution snapshot remains unchanged
        TestCaseExecutions reloadedLink = linkRepository.findById(link.getId()).orElseThrow();
        assertThat(reloadedLink.getCaseSnapshot()).contains("http://localhost/original");
        assertThat(reloadedLink.getCaseSnapshot()).doesNotContain("MODIFIED_PATH");
    }

    @Test
    @DisplayName("Verify unauthorized user cannot run automation or access jobs (access rejection)")
    void testAccessRejection() throws Exception {
        // Create active plan in project
        TestExecutions plan = new TestExecutions();
        plan.setExecutionName("Secured Plan " + UUID.randomUUID());
        plan.setExecutionStatus(ExecutionStatus.IN_PROGRESS);
        plan.setProjects(project);
        plan = testPlanRepository.save(plan);

        TestCase tc = createTestCase(project, TestTypes.CUCUMBER_AUTOMATION,
                "{\"order\":1,\"keyword\":\"Given\",\"template\":\"OPEN_PAGE\",\"textValue\":\"http://localhost/test\"}");

        TestCaseExecutions link = new TestCaseExecutions();
        link.setTestExecutions(plan);
        link.setTestCase(tc);
        link.setResultStatus(TestResult.NOT_RUN);
        linkRepository.save(link);

        // Outsider attempts to queue run -> 403 Forbidden
        mvc.perform(post("/automation/jobs/plan/" + plan.getId() + "/case/" + tc.getId() + "/run")
                        .header("Authorization", outsiderToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Verify standalone project ZIP export contains Maven wrapper, framework code, and definitions")
    void testStandaloneProjectExport() throws IOException {
        TestCase tc = createTestCase(project, TestTypes.CUCUMBER_AUTOMATION,
                "{\"order\":1,\"keyword\":\"Given\",\"template\":\"OPEN_PAGE\",\"textValue\":\"http://localhost/export-test\"}");

        // Authenticate as manager
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        manager.getUserName(), null, List.of(new SimpleGrantedAuthority("ROLE_QAManager"))
                )
        );

        byte[] zipBytes = exportService.exportProjectZip(project.getId(), List.of(tc.getId()));
        assertThat(zipBytes).isNotNull();
        assertThat(zipBytes.length).isGreaterThan(1000);

        Set<String> entries = new HashSet<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entries.add(entry.getName());
            }
        }

        assertThat(entries).contains(
                "pom.xml",
                "mvnw",
                "mvnw.cmd",
                "README.md",
                "manifest.json",
                "src/test/resources/locators.json",
                "src/test/resources/config.properties.example",
                "src/test/java/com/tcms/standalone/StandaloneDriver.java",
                "src/test/java/com/tcms/standalone/StandaloneSteps.java",
                "src/test/java/com/tcms/standalone/CucumberRunnerTest.java",
                "src/test/java/com/tcms/standalone/KeywordRunnerTest.java"
        );

        // Verify feature file was generated
        assertThat(entries).anyMatch(e -> e.startsWith("src/test/resources/features/") && e.endsWith(".feature"));
    }
    @Autowired private tools.jackson.databind.ObjectMapper mapper;
    @Autowired private ProjectVariableService variableService;
    @Autowired private org.springframework.core.env.ConfigurableEnvironment environment;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;

    private com.sun.net.httpserver.HttpServer standaloneTarget;

    @org.junit.jupiter.api.AfterEach
    void clearAuthentication() {
        if (standaloneTarget != null) standaloneTarget.stop(0);
        SecurityContextHolder.clearContext();
        environment.getPropertySources().remove("automation-worker-regression");
    }

    private void authenticateManager() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                manager.getUserName(), null, List.of(new SimpleGrantedAuthority("ROLE_QAManager"))));
    }

    private AutomationJob queuedJob() { return queuedJob("http://localhost/test"); }

    private AutomationJob queuedJob(String url) {
        TestCase tc = createTestCase(project, TestTypes.KEYWORD_DRIVEN,
                "{\"order\":1,\"template\":\"OPEN_PAGE\",\"textValue\":\"http://localhost/test\"}".replace("http://localhost/test",url));
        TestExecutions plan = new TestExecutions();
        plan.setExecutionName("Regression plan "+UUID.randomUUID()); plan.setProjects(project); plan.setExecutionStatus(ExecutionStatus.IN_PROGRESS);
        testPlanRepository.save(plan);
        TestCaseExecutions link = new TestCaseExecutions(); link.setTestExecutions(plan); link.setTestCase(tc); link.setResultStatus(TestResult.NOT_RUN);
        linkRepository.save(link); authenticateManager();
        return jobService.createRunJob(plan.getId(),tc.getId());
    }

    private AutomationExecutionResult passedResult() {
        return AutomationExecutionResult.builder().status(JobStatus.COMPLETED).durationMs(1)
                .stepResults(List.of(AutomationExecutionResult.StepResult.builder().stepOrder(1).passed(true).build())).build();
    }

    private void expectHttp(int code, org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOfSatisfying(org.springframework.web.server.ResponseStatusException.class,
                ex -> assertThat(ex.getStatusCode().value()).isEqualTo(code));
    }

    @Test
    void secretsAreMaskedWithoutChangingStoredValues() throws Exception {
        authenticateManager();
        ProjectVariable request = new ProjectVariable(); request.setVariableKey("PASSWORD"); request.setVariableValue("regression-private-value"); request.setIsSecret(true);
        var saved = variableService.saveVariable(project.getId(),request);
        assertThat(saved.variableValue()).isEqualTo("[CONFIGURED]");
        assertThat(variableService.getProjectVariables(project.getId())).extracting(ProjectVariableService.VariableView::variableValue).containsExactly("[CONFIGURED]");
        mvc.perform(get("/automation/variables/project/"+project.getId()).header("Authorization",managerToken))
                .andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("regression-private-value"))));
        entityManager.flush(); entityManager.clear();
        assertThat(variableRepository.findById(saved.id()).orElseThrow().getVariableValue()).isEqualTo("regression-private-value");
        authenticateManager(); request.setVariableValue("[CONFIGURED]");
        variableService.saveVariable(project.getId(),request);
        entityManager.flush(); entityManager.clear();
        assertThat(variableRepository.findById(saved.id()).orElseThrow().getVariableValue()).isEqualTo("regression-private-value");
    }

    @Test
    void foreignProjectCannotModifyOrDeleteLocatorsAndVariables() throws Exception {
        authenticateManager(); ProjectVariable request=new ProjectVariable(); request.setVariableKey("key"); request.setVariableValue("value");
        var variable=variableService.saveVariable(project.getId(),request);
        ProjectLocator locator=new ProjectLocator(); locator.setProject(project); locator.setLocatorName("Button"); locator.setLocatorType(LocatorType.CSS); locator.setLocatorValue("#original");
        locatorRepository.saveAndFlush(locator);
        mvc.perform(delete("/automation/variables/"+variable.id()).header("Authorization",outsiderToken)).andExpect(status().isForbidden());
        mvc.perform(put("/automation/locators/"+locator.getId()).header("Authorization",outsiderToken).contentType(MediaType.APPLICATION_JSON).content("{\"locatorValue\":\"#changed\"}")).andExpect(status().isForbidden());
        mvc.perform(delete("/automation/locators/"+locator.getId()).header("Authorization",outsiderToken)).andExpect(status().isForbidden());
        AutomationJob job=queuedJob();
        mvc.perform(post("/automation/jobs/"+job.getId()+"/cancel").header("Authorization",outsiderToken)).andExpect(status().isForbidden());
        assertThat(job.getJobStatus()).isEqualTo(JobStatus.QUEUED);
        assertThat(variableRepository.existsById(variable.id())).isTrue();
        assertThat(locatorRepository.findById(locator.getId()).orElseThrow().getLocatorValue()).isEqualTo("#original");
    }

    @Test
    void workerEndpointRequiresConfiguredCredentialAndClaimLease() throws Exception {
        AutomationJob job=queuedJob();
        mvc.perform(post("/automation/worker/jobs/poll").header("X-Worker-Token","tcms-automation-worker-token-secret")).andExpect(status().isServiceUnavailable());
        String token="regression-worker-token-with-at-least-32-characters";
        environment.getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("automation-worker-regression",Map.of(
                "automation.worker.token",token,"automation.worker.project-id",project.getId())));
        mvc.perform(post("/automation/worker/jobs/poll")).andExpect(status().isUnauthorized());
        mvc.perform(post("/automation/worker/jobs/poll").header("X-Worker-Token","tcms-automation-worker-token-secret")).andExpect(status().isUnauthorized());
        var response=mvc.perform(post("/automation/worker/jobs/poll").param("workerId","worker-a").header("X-Worker-Token",token)).andExpect(status().isOk()).andReturn();
        String lease=mapper.readTree(response.getResponse().getContentAsString()).get("leaseToken").asText();
        mvc.perform(post("/automation/worker/jobs/"+job.getId()+"/heartbeat").param("workerId","worker-a").header("X-Worker-Token",token)).andExpect(status().isForbidden());
        mvc.perform(post("/automation/worker/jobs/"+job.getId()+"/heartbeat").param("workerId","worker-a").header("X-Worker-Token",token).header("X-Job-Lease",lease)).andExpect(status().isOk());
    }

    @Test
    void projectScopeLeaseRotationAndDuplicateCompletionAreEnforced() {
        AutomationJob job=queuedJob();
        expectHttp(409,()->jobService.createRunJob(job.getPlanId(),job.getCaseId()));
        assertThat(jobService.pollNextJob(foreignProject.getId(),"worker-a")).isEmpty();
        var first=jobService.pollNextJob(project.getId(),"worker-a").orElseThrow();
        expectHttp(404,()->jobService.heartbeat(job.getId(),foreignProject.getId(),"worker-a",first.leaseToken()));
        job.setLeaseExpiration(java.time.Instant.now().minusSeconds(1)); jobRepository.flush();
        expectHttp(409,()->jobService.completeJob(job.getId(),project.getId(),"worker-a",first.leaseToken(),passedResult()));
        var second=jobService.pollNextJob(project.getId(),"worker-a").orElseThrow();
        assertThat(second.leaseToken()).isNotEqualTo(first.leaseToken());
        expectHttp(403,()->jobService.completeJob(job.getId(),project.getId(),"worker-a",first.leaseToken(),passedResult()));
        jobService.completeJob(job.getId(),project.getId(),"worker-a",second.leaseToken(),passedResult());
        jobService.completeJob(job.getId(),project.getId(),"worker-a",second.leaseToken(),AutomationExecutionResult.builder().status(JobStatus.FAILED).build());
        assertThat(job.getCaseExecution().getResultStatus()).isEqualTo(TestResult.PASS);
        assertThat(job.getCaseExecution().getResultRevision()).isEqualTo(1);
        assertThat(job.getCaseExecution().getHistory()).hasSize(1);
    }

    @Test
    void changedManualResultAndClosedPlanRejectLateCallbacks() {
        AutomationJob manual=queuedJob(); var first=jobService.pollNextJob(project.getId(),"worker-a").orElseThrow();
        manual.getCaseExecution().setResultStatus(TestResult.FAIL); manual.getCaseExecution().setResultRevision(1L); entityManager.flush();
        expectHttp(409,()->jobService.completeJob(manual.getId(),project.getId(),"worker-a",first.leaseToken(),passedResult()));
        assertThat(manual.getCaseExecution().getResultStatus()).isEqualTo(TestResult.FAIL);
        assertThat(manual.getJobStatus()).isEqualTo(JobStatus.CANCELLED);
        AutomationJob closed=queuedJob(); var second=jobService.pollNextJob(project.getId(),"worker-b").orElseThrow();
        closed.getCaseExecution().getTestExecutions().setExecutionStatus(ExecutionStatus.CLOSED); entityManager.flush();
        expectHttp(409,()->jobService.completeJob(closed.getId(),project.getId(),"worker-b",second.leaseToken(),passedResult()));
        assertThat(closed.getCaseExecution().getResultStatus()).isEqualTo(TestResult.NOT_RUN);
        assertThat(closed.getCaseExecution().getHistory()).isEmpty();
    }

    @Test
    void supersededLegacyJobCannotOverwriteNewerPass() {
        AutomationJob older=queuedJob(); var first=jobService.pollNextJob(project.getId(),"worker-a").orElseThrow();
        // Reproduce jobs created concurrently by the old implementation before the upgrade.
        AutomationJob newer=new AutomationJob(); newer.setCaseExecution(older.getCaseExecution()); newer.setPlanId(older.getPlanId()); newer.setCaseId(older.getCaseId()); newer.setProjectId(project.getId());
        newer.setTestType(older.getTestType()); newer.setTestDefinition(older.getTestDefinition()); newer.setExpectedResultRevision(0L); newer.setCreatedAt(java.time.Instant.now()); jobRepository.saveAndFlush(newer);
        var second=jobService.pollNextJob(project.getId(),"worker-b").orElseThrow();
        jobService.completeJob(newer.getId(),project.getId(),"worker-b",second.leaseToken(),passedResult());
        expectHttp(409,()->jobService.completeJob(older.getId(),project.getId(),"worker-a",first.leaseToken(),AutomationExecutionResult.builder().status(JobStatus.FAILED).build()));
        assertThat(older.getCaseExecution().getResultStatus()).isEqualTo(TestResult.PASS);
        assertThat(older.getCaseExecution().getHistory()).hasSize(1);
    }

    @Test
    void invalidPassingResultIsRejectedAndSecretsAreRedactedAtIngestion() {
        authenticateManager(); ProjectVariable secret=new ProjectVariable(); secret.setVariableKey("PASSWORD"); secret.setVariableValue("private-worker-output"); secret.setIsSecret(true); variableService.saveVariable(project.getId(),secret);
        AutomationJob job=queuedJob(); var claim=jobService.pollNextJob(project.getId(),"worker-a").orElseThrow();
        expectHttp(400,()->jobService.completeJob(job.getId(),project.getId(),"worker-a",claim.leaseToken(),AutomationExecutionResult.builder().status(JobStatus.COMPLETED).build()));
        var result=AutomationExecutionResult.builder().status(JobStatus.FAILED).errorMessage("private-worker-output").diagnosticLogs("private-worker-output")
                .stepResults(List.of(AutomationExecutionResult.StepResult.builder().description("private-worker-output").errorMessage("private-worker-output").passed(false).build())).build();
        jobService.completeJob(job.getId(),project.getId(),"worker-a",claim.leaseToken(),result);
        assertThat(job.getErrorMessage()).isEqualTo("[REDACTED]");
        assertThat(job.getDiagnosticLogs()).isEqualTo("[REDACTED]");
        assertThat(job.getStepResultsJson()).doesNotContain("private-worker-output");
    }

    @Autowired private org.springframework.transaction.PlatformTransactionManager transactionManager;

    @Test
    @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    @org.springframework.test.annotation.DirtiesContext(methodMode=org.springframework.test.annotation.DirtiesContext.MethodMode.AFTER_METHOD)
    void simultaneousCallbacksRecordOnceAndRealWorkerCanPublish() throws Exception {
        var transactions=new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        AutomationJob first=transactions.execute(status -> queuedJob());
        var claim=jobService.pollNextJob(project.getId(),"worker-a").orElseThrow();
        var start=new java.util.concurrent.CountDownLatch(1);
        try (var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Void> callback=()->{
                start.await(); jobService.completeJob(first.getId(),project.getId(),"worker-a",claim.leaseToken(),passedResult()); return null;
            };
            var a=pool.submit(callback); var b=pool.submit(callback); start.countDown();
            a.get(20,java.util.concurrent.TimeUnit.SECONDS); b.get(20,java.util.concurrent.TimeUnit.SECONDS);
        }
        transactions.executeWithoutResult(status -> {
            var link=jobRepository.findById(first.getId()).orElseThrow().getCaseExecution();
            assertThat(link.getHistory()).hasSize(1); assertThat(link.getResultRevision()).isEqualTo(1L);
        });
        String token="regression-worker-token-with-at-least-32-characters";
        environment.getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("automation-worker-regression",Map.of(
                "automation.worker.token",token,"automation.worker.project-id",project.getId())));
        AutomationJob real=transactions.execute(status -> queuedJob("http://localhost:"+port+"/demo/login"));
        var worker=new com.tcms.automation.worker.AutomationWorkerMain("http://localhost:"+port,token,"real-worker",true,100);
        assertThat(worker.pollAndExecute()).isTrue();
        transactions.executeWithoutResult(status -> {
            var job=jobRepository.findById(real.getId()).orElseThrow();
            assertThat(job.getJobStatus()).isEqualTo(JobStatus.COMPLETED);
            assertThat(job.getCaseExecution().getResultStatus()).isEqualTo(TestResult.PASS);
            assertThat(job.getCaseExecution().getHistory()).hasSize(1);
        });
    }

    @Test
    void downloadedProjectRunsBothFormatsAndRejectsWrongExpectations() throws Exception {
        // The exported project targets a separate application, without using any TMS API.
        standaloneTarget=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
        standaloneTarget.createContext("/login", exchange -> {
            byte[] html="<html><body><button id='login-btn'>Sign In</button></body></html>".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200,html.length);
            try (var output=exchange.getResponseBody()) { output.write(html); }
        });
        standaloneTarget.start();
        String url="http://127.0.0.1:"+standaloneTarget.getAddress().getPort()+"/login";
        List<Integer> ids=new ArrayList<>();
        for (TestTypes type : List.of(TestTypes.CUCUMBER_AUTOMATION,TestTypes.KEYWORD_DRIVEN)) {
            TestCase tc=createTestCase(project,type,"{\"order\":1,\"keyword\":\"Given\",\"template\":\"OPEN_PAGE\",\"textValue\":\""+url+"\"}");
            TestSteps step=new TestSteps(); step.setTestCase(tc); step.setTestType(type); step.setTestStepOrder(2); step.setTestStepDesc("Verify login form"); step.setTestExpectedOutput("");
            step.setTestStepData("{\"order\":2,\"keyword\":\"Then\",\"template\":\"VERIFY_TEXT_EQUALS\",\"locatorValue\":\"#login-btn\",\"textValue\":\"Sign In\"}");
            step.setTsCreatedDate(tc.getTestCreatedDate()); step.setTsModifiedDate(tc.getTestCreatedDate()); step.setTsCreatedBy("QA"); step.setTsModifiedBy("QA");
            tc.getTestStepsSet().add(step); testCaseRepository.save(tc); entityManager.flush(); ids.add(tc.getId());
        }
        java.nio.file.Path folder=java.nio.file.Files.createTempDirectory("tcms-standalone-regression-");
        byte[] bytes=exportService.exportProjectZip(project.getId(),ids);
        java.nio.file.Path archive=folder.resolve("export.zip"); java.nio.file.Files.write(archive,bytes);
        try (var zip=org.apache.commons.compress.archivers.zip.ZipFile.builder().setPath(archive).get()) {
            assertThat(zip.getEntry("mvnw").getUnixMode() & 0111).isEqualTo(0111);
            var entries=zip.getEntries();
            while (entries.hasMoreElements()) {
                var entry=entries.nextElement(); var output=folder.resolve(entry.getName()).normalize();
                assertThat(output.startsWith(folder)).isTrue(); java.nio.file.Files.createDirectories(output.getParent());
                try (var input=zip.getInputStream(entry)) { java.nio.file.Files.copy(input,output); }
            }
        }
        java.nio.file.Path log=folder.resolve("test-output.log");
        try {
            assertThat(runDownloadedProject(folder,log)).withFailMessage(()->readOutput(log)).isZero();
            java.nio.file.Files.copy(log,folder.resolve("passing-test-output.log"));
            String reports=readReports(folder);
            assertThat(reports).contains("CucumberRunnerTest","KeywordRunnerTest");
            // Both runners must fail on a real bad expectation, rather than silently skip their cases.
            try (var files=java.nio.file.Files.walk(folder.resolve("src/test/resources"))) {
                for (var file : files.filter(java.nio.file.Files::isRegularFile).toList()) {
                    if (file.toString().endsWith(".feature") || file.toString().endsWith(".json")) {
                        java.nio.file.Files.writeString(file,java.nio.file.Files.readString(file).replace("Sign In","Incorrect expectation"));
                    }
                }
            }
            assertThat(runDownloadedProject(folder,log)).isNotZero();
            assertThat(readOutput(log)).contains("Failures: 2");
        } finally {
            System.out.println("Standalone export verification: "+folder);
        }
    }

    private int runDownloadedProject(java.nio.file.Path folder,java.nio.file.Path log) throws Exception {
        boolean windows=System.getProperty("os.name").toLowerCase().contains("win");
        // These generated projects use public dependencies; keep the check independent of personal Maven profiles.
        var settings=folder.resolve("verification-settings.xml");
        java.nio.file.Files.writeString(settings,"<settings xmlns=\"http://maven.apache.org/SETTINGS/1.0.0\"/>");
        var process=new ProcessBuilder(windows ? List.of("cmd","/c","mvnw.cmd","-B","-s",settings.toString(),"test") : List.of("sh","./mvnw","-B","-s",settings.toString(),"test"))
                .directory(folder.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            if (!process.waitFor(180,java.util.concurrent.TimeUnit.SECONDS)) throw new AssertionError("Standalone build timed out: "+folder);
            return process.exitValue();
        } finally { if (process.isAlive()) process.destroyForcibly(); }
    }

    private String readOutput(java.nio.file.Path path) {
        try { return java.nio.file.Files.readString(path); } catch (IOException e) { return e.toString(); }
    }
    private String readReports(java.nio.file.Path folder) throws IOException {
        StringBuilder result=new StringBuilder();
        try (var files=java.nio.file.Files.list(folder.resolve("target/surefire-reports"))) {
            for (var file : files.filter(p -> p.toString().endsWith(".txt")).toList()) result.append(java.nio.file.Files.readString(file));
        }
        return result.toString();
    }

}
