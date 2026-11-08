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

        List<String> progress = new ArrayList<>();
        List<byte[]> frames = new ArrayList<>();
        AutomationExecutionResult result = cucumberRunner.execute(def, () -> false, (index, state, completed, driver) -> {
            progress.add(index + ":" + state + ":" + completed.size());
            if (index == 0 && state.equals("PASSED")) frames.add(driver.takePreviewScreenshot());
        });
        assertThat(progress).hasSize(12).startsWith("0:RUNNING:0", "0:PASSED:1").endsWith("5:PASSED:6");
        assertThat(frames).hasSize(1);
        assertThat(frames.getFirst()).hasSizeGreaterThan(100);

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

        List<String> progress = new ArrayList<>();
        AutomationExecutionResult result = keywordRunner.execute(def, () -> false, (index, state, completed, driver) ->
                progress.add(index + ":" + state + ":" + completed.size()));
        assertThat(progress).hasSize(12).startsWith("0:RUNNING:0", "0:PASSED:1").endsWith("5:PASSED:6");

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
                .locatorValue("${usernameLocator}")
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
        variables.put("usernameLocator", "#username");
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
    @DisplayName("Verify actual browser execution of all five new templates in Cucumber Automation mode against demo app")
    void testCucumberAutomationExtendedControls() {
        String baseUrl = "http://localhost:" + port;
        List<AutomationStepModel> steps = new ArrayList<>();
        int order = 1;

        steps.add(AutomationStepModel.builder().order(order++).keyword("Given").template(AutomationTemplate.OPEN_PAGE)
                .textValue(baseUrl + "/demo/extended").build());

        steps.add(AutomationStepModel.builder().order(order++).keyword("When").template(AutomationTemplate.SELECT_OPTION)
                .locatorType(LocatorType.CSS).locatorValue("#framework-select").textValue("Playwright").selectBy(OptionSelectBy.LABEL).build());
        steps.add(AutomationStepModel.builder().order(order++).keyword("Then").template(AutomationTemplate.VERIFY_TEXT_EQUALS)
                .locatorType(LocatorType.CSS).locatorValue("#selected-framework").textValue("Playwright").build());

        steps.add(AutomationStepModel.builder().order(order++).keyword("When").template(AutomationTemplate.SELECT_OPTION)
                .locatorType(LocatorType.CSS).locatorValue("#framework-select").textValue("cyp").selectBy(OptionSelectBy.VALUE).build());
        steps.add(AutomationStepModel.builder().order(order++).keyword("Then").template(AutomationTemplate.VERIFY_TEXT_EQUALS)
                .locatorType(LocatorType.CSS).locatorValue("#selected-framework").textValue("Cypress").build());

        steps.add(AutomationStepModel.builder().order(order++).keyword("When").template(AutomationTemplate.SET_CHECKBOX)
                .locatorType(LocatorType.CSS).locatorValue("#subscribe-checkbox").checkedValue(true).build());
        steps.add(AutomationStepModel.builder().order(order++).keyword("Then").template(AutomationTemplate.VERIFY_TEXT_EQUALS)
                .locatorType(LocatorType.CSS).locatorValue("#checkbox-status").textValue("Checked").build());

        steps.add(AutomationStepModel.builder().order(order++).keyword("When").template(AutomationTemplate.SET_CHECKBOX)
                .locatorType(LocatorType.CSS).locatorValue("#subscribe-checkbox").checkedValue(true).build());
        steps.add(AutomationStepModel.builder().order(order++).keyword("Then").template(AutomationTemplate.VERIFY_TEXT_EQUALS)
                .locatorType(LocatorType.CSS).locatorValue("#checkbox-status").textValue("Checked").build());

        steps.add(AutomationStepModel.builder().order(order++).keyword("When").template(AutomationTemplate.SET_CHECKBOX)
                .locatorType(LocatorType.CSS).locatorValue("#subscribe-checkbox").checkedValue(false).build());
        steps.add(AutomationStepModel.builder().order(order++).keyword("Then").template(AutomationTemplate.VERIFY_TEXT_EQUALS)
                .locatorType(LocatorType.CSS).locatorValue("#checkbox-status").textValue("Unchecked").build());

        steps.add(AutomationStepModel.builder().order(order++).keyword("When").template(AutomationTemplate.ENTER_TEXT)
                .locatorType(LocatorType.CSS).locatorValue("#keyboard-input").textValue("Playwright keyboard test").build());
        steps.add(AutomationStepModel.builder().order(order++).keyword("When").template(AutomationTemplate.PRESS_KEY)
                .locatorType(LocatorType.CSS).locatorValue("#keyboard-input").keyValue("Enter").build());
        steps.add(AutomationStepModel.builder().order(order++).keyword("Then").template(AutomationTemplate.VERIFY_TEXT_EQUALS)
                .locatorType(LocatorType.CSS).locatorValue("#submitted-query").textValue("Playwright keyboard test").build());
        steps.add(AutomationStepModel.builder().order(order++).keyword("Then").template(AutomationTemplate.VERIFY_TEXT_EQUALS)
                .locatorType(LocatorType.CSS).locatorValue("#last-key").textValue("Enter").build());

        steps.add(AutomationStepModel.builder().order(order++).keyword("Then").template(AutomationTemplate.VERIFY_ATTRIBUTE)
                .locatorType(LocatorType.CSS).locatorValue("#action-btn").attributeName("data-status").textValue("ready").build());
        steps.add(AutomationStepModel.builder().order(order++).keyword("Then").template(AutomationTemplate.VERIFY_ATTRIBUTE)
                .locatorType(LocatorType.CSS).locatorValue("#action-btn").attributeName("data-empty").textValue("").build());

        steps.add(AutomationStepModel.builder().order(order++).keyword("Then").template(AutomationTemplate.VERIFY_URL)
                .urlComparison(UrlComparison.EQUALS).textValue(baseUrl + "/demo/extended").build());
        steps.add(AutomationStepModel.builder().order(order++).keyword("Then").template(AutomationTemplate.VERIFY_URL)
                .urlComparison(UrlComparison.CONTAINS).textValue("/demo/extended").build());

        steps.add(AutomationStepModel.builder().order(order++).keyword("When").template(AutomationTemplate.CLICK_ELEMENT)
                .locatorType(LocatorType.CSS).locatorValue("#nav-login-link").build());
        steps.add(AutomationStepModel.builder().order(order++).keyword("Then").template(AutomationTemplate.VERIFY_URL)
                .urlComparison(UrlComparison.CONTAINS).textValue("/demo/login").build());

        AutomationTestDefinition def = AutomationTestDefinition.builder()
                .caseId(201)
                .caseName("Extended Controls Cucumber Scenario")
                .testType(TestTypes.CUCUMBER_AUTOMATION)
                .steps(steps)
                .executionConfig(Map.of("stepTimeoutMs", "4000", "headless", "true"))
                .build();

        AutomationExecutionResult result = cucumberRunner.execute(def);

        assertThat(result.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(result.getStepResults()).hasSize(steps.size());
        assertThat(result.getStepResults()).allMatch(AutomationExecutionResult.StepResult::isPassed);
        assertThat(result.getFailureScreenshot()).isNull();
    }

    @Test
    @DisplayName("Verify actual browser execution of all five new templates in Keyword Driven mode against demo app")
    void testKeywordDrivenExtendedControls() {
        String baseUrl = "http://localhost:" + port;
        List<AutomationStepModel> steps = new ArrayList<>();
        int order = 1;

        steps.add(AutomationStepModel.builder().order(order++).template(AutomationTemplate.OPEN_PAGE)
                .textValue(baseUrl + "/demo/extended").build());
        steps.add(AutomationStepModel.builder().order(order++).template(AutomationTemplate.SELECT_OPTION)
                .locatorType(LocatorType.CSS).locatorValue("#framework-select").textValue("Playwright").selectBy(OptionSelectBy.LABEL).build());
        steps.add(AutomationStepModel.builder().order(order++).template(AutomationTemplate.VERIFY_TEXT_EQUALS)
                .locatorType(LocatorType.CSS).locatorValue("#selected-framework").textValue("Playwright").build());
        steps.add(AutomationStepModel.builder().order(order++).template(AutomationTemplate.SET_CHECKBOX)
                .locatorType(LocatorType.CSS).locatorValue("#subscribe-checkbox").checkedValue(true).build());
        steps.add(AutomationStepModel.builder().order(order++).template(AutomationTemplate.VERIFY_TEXT_EQUALS)
                .locatorType(LocatorType.CSS).locatorValue("#checkbox-status").textValue("Checked").build());
        steps.add(AutomationStepModel.builder().order(order++).template(AutomationTemplate.ENTER_TEXT)
                .locatorType(LocatorType.CSS).locatorValue("#keyboard-input").textValue("Keyword press").build());
        steps.add(AutomationStepModel.builder().order(order++).template(AutomationTemplate.PRESS_KEY)
                .locatorType(LocatorType.CSS).locatorValue("#keyboard-input").keyValue("Enter").build());
        steps.add(AutomationStepModel.builder().order(order++).template(AutomationTemplate.VERIFY_TEXT_EQUALS)
                .locatorType(LocatorType.CSS).locatorValue("#submitted-query").textValue("Keyword press").build());
        steps.add(AutomationStepModel.builder().order(order++).template(AutomationTemplate.VERIFY_ATTRIBUTE)
                .locatorType(LocatorType.CSS).locatorValue("#action-btn").attributeName("data-status").textValue("ready").build());
        steps.add(AutomationStepModel.builder().order(order++).template(AutomationTemplate.VERIFY_URL)
                .urlComparison(UrlComparison.CONTAINS).textValue("/demo/extended").build());

        AutomationTestDefinition def = AutomationTestDefinition.builder()
                .caseId(202)
                .caseName("Extended Controls Keyword Scenario")
                .testType(TestTypes.KEYWORD_DRIVEN)
                .steps(steps)
                .executionConfig(Map.of("stepTimeoutMs", "4000", "headless", "true"))
                .build();

        AutomationExecutionResult result = keywordRunner.execute(def);

        assertThat(result.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(result.getStepResults()).hasSize(steps.size());
        assertThat(result.getStepResults()).allMatch(AutomationExecutionResult.StepResult::isPassed);
        assertThat(result.getFailureScreenshot()).isNull();
    }

    @Test
    @DisplayName("Verify genuine assertion failures and validation errors for the five new templates")
    void testExtendedAssertionFailuresAndValidation() {
        String baseUrl = "http://localhost:" + port;

        // 1. Missing attribute distinguishes from empty attribute value
        List<AutomationStepModel> missingAttrSteps = List.of(
                AutomationStepModel.builder().order(1).template(AutomationTemplate.OPEN_PAGE).textValue(baseUrl + "/demo/extended").build(),
                AutomationStepModel.builder().order(2).template(AutomationTemplate.VERIFY_ATTRIBUTE)
                        .locatorType(LocatorType.CSS).locatorValue("#action-btn").attributeName("data-nonexistent").textValue("any").build()
        );
        AutomationExecutionResult missingAttrRes = keywordRunner.execute(AutomationTestDefinition.builder()
                .caseId(203).caseName("Missing Attribute Test").testType(TestTypes.KEYWORD_DRIVEN).steps(missingAttrSteps)
                .executionConfig(Map.of("stepTimeoutMs", "1000", "headless", "true")).build());
        assertThat(missingAttrRes.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(missingAttrRes.getErrorMessage()).contains("does not have attribute 'data-nonexistent'");
        assertThat(missingAttrRes.getFailureScreenshot()).isNotNull();

        // 2. Mismatched URL assertion failure
        List<AutomationStepModel> urlFailSteps = List.of(
                AutomationStepModel.builder().order(1).template(AutomationTemplate.OPEN_PAGE).textValue(baseUrl + "/demo/extended").build(),
                AutomationStepModel.builder().order(2).template(AutomationTemplate.VERIFY_URL)
                        .urlComparison(UrlComparison.EQUALS).textValue(baseUrl + "/wrong-path").build()
        );
        AutomationExecutionResult urlFailRes = keywordRunner.execute(AutomationTestDefinition.builder()
                .caseId(204).caseName("URL Mismatch Test").testType(TestTypes.KEYWORD_DRIVEN).steps(urlFailSteps)
                .executionConfig(Map.of("stepTimeoutMs", "1000", "headless", "true")).build());
        assertThat(urlFailRes.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(urlFailRes.getErrorMessage()).contains("URL verification failed");

        // 3. Unsupported key validation error
        List<AutomationStepModel> badKeySteps = List.of(
                AutomationStepModel.builder().order(1).template(AutomationTemplate.OPEN_PAGE).textValue(baseUrl + "/demo/extended").build(),
                AutomationStepModel.builder().order(2).template(AutomationTemplate.PRESS_KEY)
                        .locatorType(LocatorType.CSS).locatorValue("#keyboard-input").keyValue("InvalidKeyXYZ").build()
        );
        AutomationExecutionResult badKeyRes = keywordRunner.execute(AutomationTestDefinition.builder()
                .caseId(205).caseName("Bad Key Test").testType(TestTypes.KEYWORD_DRIVEN).steps(badKeySteps)
                .executionConfig(Map.of("stepTimeoutMs", "1000", "headless", "true")).build());
        assertThat(badKeyRes.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(badKeyRes.getErrorMessage()).contains("Unsupported key 'InvalidKeyXYZ'");
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

    @Autowired private AutomationJobPreviewRepository previewRepository;

    @Test
    void previewIsProjectProtectedRedactedAndDoesNotRecordResults() throws Exception {
        AutomationJob job = queuedJob("http://localhost/preview-private-value");
        var definition = mapper.readValue(job.getTestDefinition(), AutomationTestDefinition.class);
        definition.setVariables(Map.of("PASSWORD", "preview-private-value")); definition.setSecretKeys(Set.of("PASSWORD"));
        job.setTestDefinition(mapper.writeValueAsString(definition));
        var claim = jobService.pollNextJob(project.getId(), "preview-worker").orElseThrow();
        jobService.updateProgress(job.getId(), project.getId(), "preview-worker", claim.leaseToken(),
                new AutomationProgress(1, 0, "RUNNING", List.of(), null));
        var view = jobService.getPreview(job.getId(), 0);
        assertThat(mapper.writeValueAsString(view)).contains("[REDACTED]").doesNotContain("preview-private-value");
        long revision = ((Number) view.get("sequence")).longValue();
        assertThat(jobService.getPreview(job.getId(), revision)).isNull();
        assertThat(job.getCaseExecution().getResultStatus()).isEqualTo(TestResult.NOT_RUN);
        assertThat(job.getCaseExecution().getHistory()).isEmpty();
        mvc.perform(get("/automation/jobs/" + job.getId() + "/preview").header("Authorization", outsiderToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/automation/jobs/" + job.getId() + "/preview").header("Authorization", managerToken))
                .andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Cache-Control", "no-store"));
    }

    @Test
    void previewRejectsWrongExpiredAndCancelledClaimsAndResetsOnReclaim() {
        AutomationJob job = queuedJob();
        var first = jobService.pollNextJob(project.getId(), "preview-worker").orElseThrow();
        var initial = new AutomationProgress(5, 0, "RUNNING", List.of(), null);
        expectHttp(403, () -> jobService.updateProgress(job.getId(), project.getId(), "other-worker", first.leaseToken(), initial));
        expectHttp(404, () -> jobService.updateProgress(job.getId(), foreignProject.getId(), "preview-worker", first.leaseToken(), initial));
        jobService.updateProgress(job.getId(), project.getId(), "preview-worker", first.leaseToken(), initial);
        long firstRevision = ((Number) jobService.getPreview(job.getId(), 0).get("sequence")).longValue();
        job.setLeaseExpiration(java.time.Instant.now().minusSeconds(1)); jobRepository.flush();
        assertThat(jobService.getPreview(job.getId(), 0)).isNull();
        expectHttp(409, () -> jobService.updateProgress(job.getId(), project.getId(), "preview-worker", first.leaseToken(), initial));
        var second = jobService.pollNextJob(project.getId(), "preview-worker").orElseThrow();
        assertThat(jobService.getPreview(job.getId(), 0)).isNull();
        expectHttp(403, () -> jobService.updateProgress(job.getId(), project.getId(), "preview-worker", first.leaseToken(), initial));
        jobService.updateProgress(job.getId(), project.getId(), "preview-worker", second.leaseToken(), new AutomationProgress(1, 0, "RUNNING", List.of(), null));
        assertThat(((Number) jobService.getPreview(job.getId(), firstRevision).get("sequence")).longValue()).isGreaterThan(firstRevision);
        jobService.cancelJob(job.getId());
        assertThat(previewRepository.existsById(job.getId())).isFalse();
        expectHttp(409, () -> jobService.updateProgress(job.getId(), project.getId(), "preview-worker", second.leaseToken(), initial));
    }

    @Test
    void previewValidatesPayloadsIgnoresOlderFramesAndExpires() {
        AutomationJob job = queuedJob();
        var claim = jobService.pollNextJob(project.getId(), "preview-worker").orElseThrow();
        java.util.function.Consumer<AutomationProgress> send = progress -> jobService.updateProgress(job.getId(), project.getId(), "preview-worker", claim.leaseToken(), progress);
        expectHttp(400, () -> send.accept(new AutomationProgress(1, 5, "RUNNING", List.of(), null)));
        expectHttp(400, () -> send.accept(new AutomationProgress(1, 0, "PASSED", List.of(), null)));
        expectHttp(400, () -> send.accept(new AutomationProgress(1, 0, "RUNNING", List.of(), new byte[600000])));
        expectHttp(400, () -> send.accept(new AutomationProgress(1, 0, "RUNNING", List.of(), "not an image".getBytes())));
        send.accept(new AutomationProgress(2, 0, "PASSED", passedResult().getStepResults(), null));
        var view = jobService.getPreview(job.getId(), 0);
        send.accept(new AutomationProgress(1, 0, "RUNNING", List.of(), null));
        assertThat(jobService.getPreview(job.getId(), 0)).isEqualTo(view);
        jobService.completeJob(job.getId(), project.getId(), "preview-worker", claim.leaseToken(), passedResult());
        assertThat(jobService.getPreview(job.getId(), 0)).isNotNull(); // Fast runs retain the last frame briefly.
        previewRepository.findById(job.getId()).orElseThrow().setUpdatedAt(java.time.Instant.now().minusSeconds(901));
        entityManager.flush();
        assertThat(jobService.getPreview(job.getId(), 0)).isNull();
        previewRepository.deleteExpired(java.time.Instant.now().minusSeconds(900));
        entityManager.clear();
        assertThat(previewRepository.existsById(job.getId())).isFalse();
    }

    @Test
    void previewWorkerEndpointRequiresTokenAndLease() throws Exception {
        AutomationJob job = queuedJob();
        var claim = jobService.pollNextJob(project.getId(), "preview-worker").orElseThrow();
        String token = "preview-worker-token-with-at-least-32-characters";
        environment.getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("automation-worker-regression", Map.of(
                "automation.worker.token", token, "automation.worker.project-id", project.getId())));
        String path = "/automation/worker/jobs/" + job.getId() + "/progress";
        String body = mapper.writeValueAsString(new AutomationProgress(1, 0, "RUNNING", List.of(), null));
        mvc.perform(post(path).param("workerId", "preview-worker").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(path).param("workerId", "preview-worker").header("X-Worker-Token", token).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post(path).param("workerId", "preview-worker").header("X-Worker-Token", token).header("X-Job-Lease", claim.leaseToken())
                        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
    }

    @Test
    void previewMasksFormValuesAndPreviewFailuresDoNotFailTests() throws Exception {
        var definition = AutomationTestDefinition.builder().variables(Map.of("PASSWORD", "preview-private-value"))
                .secretKeys(Set.of("PASSWORD")).build();
        try (var driver = new AutomationBrowserDriver(definition)) {
            driver.init(true);
            driver.getPage().setContent("<input id='name'><input id='password' type='password'><p>preview-private-value</p>");
            driver.getPage().locator("#name").fill("name-value");
            driver.getPage().locator("#password").fill("preview-private-value");
            byte[] frame = driver.takePreviewScreenshot();
            assertThat(frame).hasSizeGreaterThan(100);
            var image = javax.imageio.ImageIO.read(new ByteArrayInputStream(frame));
            for (String id : List.of("#name", "#password", "p")) {
                var bounds = driver.getPage().locator(id).boundingBox();
                var color = new java.awt.Color(image.getRGB((int) (bounds.x + bounds.width / 2), (int) (bounds.y + bounds.height / 2)));
                assertThat(color.getRed()).isGreaterThan(200);
                assertThat(color.getBlue()).isGreaterThan(200);
                assertThat(color.getGreen()).isLessThan(80);
            }
            assertThat(driver.getPage().locator("#password").inputValue()).isEqualTo("preview-private-value");
        }
        var run = AutomationTestDefinition.builder().caseName("Optional preview")
                .steps(List.of(AutomationStepModel.builder().order(1).template(AutomationTemplate.OPEN_PAGE)
                        .textValue("http://localhost:" + port + "/demo/login").build())).build();
        var result = keywordRunner.execute(run, () -> false, (index, state, steps, driver) -> { throw new IllegalStateException("Preview offline"); });
        assertThat(result.getStatus()).isEqualTo(JobStatus.COMPLETED);
    }

    @Test
    void navigationContinuesWhenAnUnrelatedImageHasNotFinished() throws Exception {
        var release = new java.util.concurrent.CountDownLatch(1);
        var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        var requests = java.util.concurrent.Executors.newCachedThreadPool();
        server.setExecutor(requests);
        server.createContext("/page", exchange -> {
            byte[] html = "<p id='ready'>Ready</p><img src='/slow-image'>".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, html.length);
            exchange.getResponseBody().write(html); exchange.close();
        });
        server.createContext("/slow-image", exchange -> {
            try {
                exchange.sendResponseHeaders(200, 1000);
                exchange.getResponseBody().write(1); exchange.getResponseBody().flush();
                release.await(10, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException error) { Thread.currentThread().interrupt(); }
            finally { exchange.close(); }
        });
        server.start();
        try (var driver = new AutomationBrowserDriver(AutomationTestDefinition.builder()
                .executionConfig(Map.of("stepTimeoutMs", "1000", "navigationTimeoutMs", "3000")).build())) {
            driver.init(true);
            long start = System.nanoTime();
            driver.openPage("http://127.0.0.1:" + server.getAddress().getPort() + "/page");
            driver.verifyTextEquals("#ready", "Ready");
            assertThat(java.time.Duration.ofNanos(System.nanoTime() - start)).isLessThan(java.time.Duration.ofSeconds(3));
        } finally { release.countDown(); server.stop(0); requests.shutdownNow(); }
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
            authenticateManager();
            var preview = jobService.getPreview(real.getId(), 0);
            assertThat(preview).isNotNull();
            assertThat((byte[]) preview.get("screenshot")).hasSizeGreaterThan(100);
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
        String optionLabel="Option \"B\" \\ path";
        standaloneTarget.createContext("/login", exchange -> {
            byte[] html=("<html><body>"
                    + "<select id='opts'><option value='a'>OptionA</option><option value='b'>"+optionLabel+"</option></select>"
                    + "<input type='checkbox' id='agree-box'/>"
                    + "<input type='text' id='txt'/>"
                    + "<button id='login-btn' data-status='ready'>Sign In</button>"
                    + "</body></html>").getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200,html.length);
            try (var output=exchange.getResponseBody()) { output.write(html); }
        });
        standaloneTarget.start();
        String url="http://127.0.0.1:"+standaloneTarget.getAddress().getPort()+"/login";
        ProjectVariable locatorVariable=new ProjectVariable(); locatorVariable.setProject(project); locatorVariable.setVariableKey("submitLocator"); locatorVariable.setVariableValue("#login-btn"); variableRepository.save(locatorVariable);
        List<Integer> ids=new ArrayList<>();
        for (TestTypes type : List.of(TestTypes.CUCUMBER_AUTOMATION,TestTypes.KEYWORD_DRIVEN)) {
            TestCase tc=createTestCase(project,type,"{\"order\":1,\"keyword\":\"Given\",\"template\":\"OPEN_PAGE\",\"textValue\":\""+url+"\"}");

            TestSteps step2=new TestSteps(); step2.setTestCase(tc); step2.setTestType(type); step2.setTestStepOrder(2); step2.setTestStepDesc("Select Option"); step2.setTestExpectedOutput("");
            step2.setTestStepData(mapper.writeValueAsString(AutomationStepModel.builder().order(2).keyword("When")
                    .template(AutomationTemplate.SELECT_OPTION).selectBy(OptionSelectBy.LABEL).locatorValue("#opts").textValue(optionLabel).build()));
            step2.setTsCreatedDate(tc.getTestCreatedDate()); step2.setTsModifiedDate(tc.getTestCreatedDate()); step2.setTsCreatedBy("QA"); step2.setTsModifiedBy("QA");
            tc.getTestStepsSet().add(step2);

            TestSteps step3=new TestSteps(); step3.setTestCase(tc); step3.setTestType(type); step3.setTestStepOrder(3); step3.setTestStepDesc("Check Box"); step3.setTestExpectedOutput("");
            step3.setTestStepData("{\"order\":3,\"keyword\":\"When\",\"template\":\"SET_CHECKBOX\",\"checkedValue\":true,\"locatorValue\":\"#agree-box\"}");
            step3.setTsCreatedDate(tc.getTestCreatedDate()); step3.setTsModifiedDate(tc.getTestCreatedDate()); step3.setTsCreatedBy("QA"); step3.setTsModifiedBy("QA");
            tc.getTestStepsSet().add(step3);

            TestSteps step4=new TestSteps(); step4.setTestCase(tc); step4.setTestType(type); step4.setTestStepOrder(4); step4.setTestStepDesc("Press Enter"); step4.setTestExpectedOutput("");
            step4.setTestStepData("{\"order\":4,\"keyword\":\"When\",\"template\":\"PRESS_KEY\",\"keyValue\":\"enter\",\"locatorValue\":\"#txt\"}");
            step4.setTsCreatedDate(tc.getTestCreatedDate()); step4.setTsModifiedDate(tc.getTestCreatedDate()); step4.setTsCreatedBy("QA"); step4.setTsModifiedBy("QA");
            tc.getTestStepsSet().add(step4);

            TestSteps step5=new TestSteps(); step5.setTestCase(tc); step5.setTestType(type); step5.setTestStepOrder(5); step5.setTestStepDesc("Verify URL"); step5.setTestExpectedOutput("");
            step5.setTestStepData("{\"order\":5,\"keyword\":\"Then\",\"template\":\"VERIFY_URL\",\"urlComparison\":\"CONTAINS\",\"textValue\":\"/login\"}");
            step5.setTsCreatedDate(tc.getTestCreatedDate()); step5.setTsModifiedDate(tc.getTestCreatedDate()); step5.setTsCreatedBy("QA"); step5.setTsModifiedBy("QA");
            tc.getTestStepsSet().add(step5);

            TestSteps step6=new TestSteps(); step6.setTestCase(tc); step6.setTestType(type); step6.setTestStepOrder(6); step6.setTestStepDesc("Verify Attribute"); step6.setTestExpectedOutput("");
            step6.setTestStepData("{\"order\":6,\"keyword\":\"Then\",\"template\":\"VERIFY_ATTRIBUTE\",\"attributeName\":\"data-status\",\"locatorValue\":\"${submitLocator}\",\"textValue\":\"ready\"}");
            step6.setTsCreatedDate(tc.getTestCreatedDate()); step6.setTsModifiedDate(tc.getTestCreatedDate()); step6.setTsCreatedBy("QA"); step6.setTsModifiedBy("QA");
            tc.getTestStepsSet().add(step6);

            TestSteps step7=new TestSteps(); step7.setTestCase(tc); step7.setTestType(type); step7.setTestStepOrder(7); step7.setTestStepDesc("Verify login form"); step7.setTestExpectedOutput("");
            step7.setTestStepData("{\"order\":7,\"keyword\":\"Then\",\"template\":\"VERIFY_TEXT_EQUALS\",\"locatorValue\":\"${submitLocator}\",\"textValue\":\"Sign In\"}");
            step7.setTsCreatedDate(tc.getTestCreatedDate()); step7.setTsModifiedDate(tc.getTestCreatedDate()); step7.setTsCreatedBy("QA"); step7.setTsModifiedBy("QA");
            tc.getTestStepsSet().add(step7);

            testCaseRepository.save(tc); entityManager.flush(); ids.add(tc.getId());
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
