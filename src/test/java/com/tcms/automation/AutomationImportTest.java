package com.tcms.automation;

import tools.jackson.databind.ObjectMapper;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
public class AutomationImportTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private TestStepsRepository testStepsRepository;

    @Autowired
    private TestExecutionRepository testPlanRepository;

    @Autowired
    private TestCaseExecutionRepository linkRepository;

    @Autowired
    private ProjectLocatorRepository locatorRepository;

    @Autowired
    private ProjectVariableRepository variableRepository;

    @Autowired
    private AutomationProjectExportService exportService;

    @Autowired
    private JwtTokenUtil tokenUtil;

    @Autowired
    private CustomUserDetailService customUserDetailService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Projects project;
    private Projects foreignProject;
    private Users manager;
    private Users outsider;
    private String managerToken;
    private String outsiderToken;

    @BeforeEach
    void setUp() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> seedFixture());
    }

    private void seedFixture() {
        project = new Projects();
        project.setProjectName("ImportProj-" + UUID.randomUUID());
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

        UserDetails managerDetails = customUserDetailService.loadUserByUsername(manager.getUserName());
        managerToken = tokenUtil.generateToken(managerDetails);

        UserDetails outsiderDetails = customUserDetailService.loadUserByUsername(outsider.getUserName());
        outsiderToken = tokenUtil.generateToken(outsiderDetails);

        // Seed common project locator
        ProjectLocator loc1 = new ProjectLocator();
        loc1.setProject(project);
        loc1.setLocatorName("login.user");
        loc1.setLocatorType(LocatorType.CSS);
        loc1.setLocatorValue("#username");
        loc1.setCreatedBy(manager.getUserName());
        locatorRepository.save(loc1);

        ProjectLocator loc2 = new ProjectLocator();
        loc2.setProject(project);
        loc2.setLocatorName("login.submit");
        loc2.setLocatorType(LocatorType.CSS);
        loc2.setLocatorValue("button#submit");
        loc2.setCreatedBy(manager.getUserName());
        locatorRepository.save(loc2);

        ProjectLocator loc3 = new ProjectLocator();
        loc3.setProject(project);
        loc3.setLocatorName("welcome.msg");
        loc3.setLocatorType(LocatorType.CSS);
        loc3.setLocatorValue(".welcome-text");
        loc3.setCreatedBy(manager.getUserName());
        locatorRepository.save(loc3);
    }

    @Test
    @DisplayName("Import Cucumber feature with Background and multiple scenarios successfully")
    void testCucumberFeatureWithBackgroundAndMultipleScenarios() throws Exception {
        String featureContent = """
                Feature: User Authentication
                
                  Background:
                    Given open page "https://demo.app/login"
                
                  Scenario: Valid Login
                    When enter text "admin" into "login.user"
                    And click element "login.submit"
                    Then verify text of "welcome.msg" equals "Hello Admin"
                
                  Scenario: Visual Check
                    When click element "login.submit"
                    Then verify element "welcome.msg" is visible
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "auth.feature",
                "text/plain",
                featureContent.getBytes(StandardCharsets.UTF_8)
        );

        // 1. Preview
        String previewJson = mvc.perform(multipart("/automation/projects/" + project.getId() + "/import/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileType").value("FEATURE"))
                .andExpect(jsonPath("$.hasErrors").value(false))
                .andExpect(jsonPath("$.canSave").value(true))
                .andExpect(jsonPath("$.totalCases").value(2))
                .andExpect(jsonPath("$.cases[0].name").value("Valid Login"))
                .andExpect(jsonPath("$.cases[0].steps.length()").value(4))
                .andExpect(jsonPath("$.cases[0].steps[0].template").value("OPEN_PAGE"))
                .andExpect(jsonPath("$.cases[0].steps[0].keyword").value("Given"))
                .andExpect(jsonPath("$.cases[0].steps[1].template").value("ENTER_TEXT"))
                .andExpect(jsonPath("$.cases[1].name").value("Visual Check"))
                .andExpect(jsonPath("$.cases[1].steps.length()").value(3))
                .andExpect(jsonPath("$.cases[1].steps[0].template").value("OPEN_PAGE"))
                .andReturn().getResponse().getContentAsString();

        ImportPreviewResponse preview = mapper.readValue(previewJson, ImportPreviewResponse.class);
        assertThat(preview.getCases()).hasSize(2);

        // 2. Save
        List<ImportCaseSaveItem> saveItems = new ArrayList<>();
        for (ImportCasePreview cp : preview.getCases()) {
            List<AutomationStepModel> steps = cp.getSteps().stream().map(sp -> AutomationStepModel.builder()
                    .order(sp.getOrder())
                    .keyword(sp.getKeyword())
                    .template(sp.getTemplate())
                    .locatorType(sp.getLocatorType())
                    .locatorValue(sp.getLocatorValue())
                    .textValue(sp.getTextValue())
                    .stateValue(sp.getStateValue())
                    .build()).toList();

            saveItems.add(ImportCaseSaveItem.builder()
                    .action(ImportAction.CREATE_NEW)
                    .caseName(cp.getName())
                    .testType(cp.getTestType())
                    .steps(steps)
                    .build());
        }

        ImportSaveRequest saveRequest = ImportSaveRequest.builder().cases(saveItems).build();

        mvc.perform(post("/automation/projects/" + project.getId() + "/import/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + managerToken)
                        .content(mapper.writeValueAsString(saveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(2))
                .andExpect(jsonPath("$.replacedCount").value(0));

        List<TestCase> saved = testCaseRepository.findByProjectsIdOrderByIdAsc(project.getId());
        assertThat(saved).hasSize(2);
        assertThat(saved).extracting(TestCase::getTestName).containsExactlyInAnyOrder("Valid Login", "Visual Check");

        // Verify steps for "Valid Login"
        TestCase validLoginCase = saved.stream().filter(c -> c.getTestName().equals("Valid Login")).findFirst().orElseThrow();
        List<TestSteps> steps = testStepsRepository.findTestStepsByTestCaseAndTestTypeOrderByTestStepOrderAsc(validLoginCase, TestTypes.CUCUMBER_AUTOMATION);
        assertThat(steps).hasSize(4);
        assertThat(steps.get(0).getTestStepDesc()).contains("open page \"https://demo.app/login\"");
        assertThat(steps.get(1).getTestStepDesc()).contains("enter text \"admin\" into \"login.user\"");
    }

    @Test
    @DisplayName("Import Keyword Driven JSON successfully")
    void testKeywordJsonImport() throws Exception {
        String jsonContent = """
                [
                  {
                    "name": "Keyword Search Test",
                    "type": "KEYWORD_DRIVEN",
                    "steps": [
                      {
                        "order": 1,
                        "template": "OPEN_PAGE",
                        "textValue": "https://search.example.com"
                      },
                      {
                        "order": 2,
                        "template": "ENTER_TEXT",
                        "locatorType": "NAMED",
                        "locatorValue": "login.user",
                        "textValue": "playwright query"
                      },
                      {
                        "order": 3,
                        "template": "CLICK_ELEMENT",
                        "locatorType": "NAMED",
                        "locatorValue": "login.submit"
                      },
                      {
                        "order": 4,
                        "template": "VERIFY_ELEMENT_STATE",
                        "locatorType": "NAMED",
                        "locatorValue": "welcome.msg",
                        "stateValue": "VISIBLE"
                      }
                    ]
                  }
                ]
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "keywords.json",
                "application/json",
                jsonContent.getBytes(StandardCharsets.UTF_8)
        );

        String previewJson = mvc.perform(multipart("/automation/projects/" + project.getId() + "/import/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileType").value("KEYWORD_JSON"))
                .andExpect(jsonPath("$.hasErrors").value(false))
                .andExpect(jsonPath("$.canSave").value(true))
                .andExpect(jsonPath("$.totalCases").value(1))
                .andExpect(jsonPath("$.cases[0].steps.length()").value(4))
                .andReturn().getResponse().getContentAsString();

        ImportPreviewResponse preview = mapper.readValue(previewJson, ImportPreviewResponse.class);

        // Save
        ImportCasePreview cp = preview.getCases().get(0);
        List<AutomationStepModel> steps = cp.getSteps().stream().map(sp -> AutomationStepModel.builder()
                .order(sp.getOrder())
                .template(sp.getTemplate())
                .locatorType(sp.getLocatorType())
                .locatorValue(sp.getLocatorValue())
                .textValue(sp.getTextValue())
                .stateValue(sp.getStateValue())
                .build()).toList();

        ImportSaveRequest saveRequest = ImportSaveRequest.builder()
                .cases(List.of(ImportCaseSaveItem.builder()
                        .action(ImportAction.CREATE_NEW)
                        .caseName(cp.getName())
                        .testType(cp.getTestType())
                        .steps(steps)
                        .build()))
                .build();

        mvc.perform(post("/automation/projects/" + project.getId() + "/import/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + managerToken)
                        .content(mapper.writeValueAsString(saveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(1));

        List<TestCase> saved = testCaseRepository.findByProjectsIdOrderByIdAsc(project.getId());
        assertThat(saved).hasSize(1);
        TestCase tc = saved.get(0);
        assertThat(tc.getTestName()).isEqualTo("Keyword Search Test");
        assertThat(tc.getTestType()).isEqualTo(TestTypes.KEYWORD_DRIVEN);

        List<TestSteps> stepEntities = testStepsRepository.findTestStepsByTestCaseAndTestTypeOrderByTestStepOrderAsc(tc, TestTypes.KEYWORD_DRIVEN);
        assertThat(stepEntities).hasSize(4);
    }

    @Test
    @DisplayName("Detect unsupported syntax: Scenario Outlines, Data Tables, and Doc Strings")
    void testUnsupportedSyntaxDetection() throws Exception {
        String featureWithOutlineAndDataTable = """
                Feature: Unsupported Syntax Detection
                
                  Scenario Outline: Parameterized Login
                    When enter text "<username>" into "login.user"
                    Examples:
                      | username |
                      | admin    |
                      | user     |
                
                  Scenario: Data Table Step
                    When enter text "admin" into "login.user"
                      | extra | column |
                      | foo   | bar    |
                
                  Scenario: Doc String Step
                    When enter text "admin" into "login.user"
                      \"\"\"
                      multiline doc string
                      \"\"\"
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "unsupported.feature",
                "text/plain",
                featureWithOutlineAndDataTable.getBytes(StandardCharsets.UTF_8)
        );

        mvc.perform(multipart("/automation/projects/" + project.getId() + "/import/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasErrors").value(true))
                .andExpect(jsonPath("$.canSave").value(false))
                .andExpect(jsonPath("$.cases[0].unsupportedSyntax").value(org.hamcrest.Matchers.containsString("Scenario Outlines and Examples are not supported")))
                .andExpect(jsonPath("$.cases[1].steps[0].unsupportedSyntax").value(org.hamcrest.Matchers.containsString("Data tables are not supported")))
                .andExpect(jsonPath("$.cases[2].steps[0].unsupportedSyntax").value(org.hamcrest.Matchers.containsString("Doc strings are not supported")));
    }

    @Test
    @DisplayName("Unrecognized step is flagged as unmapped and can be mapped by user before save")
    void testUnmappedStepCorrectionFlow() throws Exception {
        String rawFeature = """
                Feature: Custom Step Test
                  Scenario: Custom Checkout
                    Given open page "https://store.example.com"
                    When do some custom magic with items
                    Then verify element "welcome.msg" is visible
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "custom.feature",
                "text/plain",
                rawFeature.getBytes(StandardCharsets.UTF_8)
        );

        // Preview should show step 2 as unmapped
        String previewJson = mvc.perform(multipart("/automation/projects/" + project.getId() + "/import/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasErrors").value(true))
                .andExpect(jsonPath("$.canSave").value(false))
                .andExpect(jsonPath("$.cases[0].steps[1].mapped").value(false))
                .andExpect(jsonPath("$.cases[0].steps[1].rawText").value("do some custom magic with items"))
                .andReturn().getResponse().getContentAsString();

        ImportPreviewResponse preview = mapper.readValue(previewJson, ImportPreviewResponse.class);
        ImportCasePreview casePreview = preview.getCases().get(0);

        // User maps the unrecognized step in UI to CLICK_ELEMENT
        List<AutomationStepModel> correctedSteps = new ArrayList<>();
        correctedSteps.add(AutomationStepModel.builder()
                .order(1)
                .keyword("Given")
                .template(AutomationTemplate.OPEN_PAGE)
                .textValue("https://store.example.com")
                .build());
        correctedSteps.add(AutomationStepModel.builder()
                .order(2)
                .keyword("When")
                .template(AutomationTemplate.CLICK_ELEMENT)
                .locatorType(LocatorType.NAMED)
                .locatorValue("login.submit") // mapped!
                .build());
        correctedSteps.add(AutomationStepModel.builder()
                .order(3)
                .keyword("Then")
                .template(AutomationTemplate.VERIFY_ELEMENT_STATE)
                .locatorType(LocatorType.NAMED)
                .locatorValue("welcome.msg")
                .stateValue(ElementState.VISIBLE)
                .build());

        ImportSaveRequest saveRequest = ImportSaveRequest.builder()
                .cases(List.of(ImportCaseSaveItem.builder()
                        .action(ImportAction.CREATE_NEW)
                        .caseName(casePreview.getName())
                        .testType(casePreview.getTestType())
                        .steps(correctedSteps)
                        .build()))
                .build();

        mvc.perform(post("/automation/projects/" + project.getId() + "/import/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + managerToken)
                        .content(mapper.writeValueAsString(saveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(1));

        List<TestCase> saved = testCaseRepository.findByProjectsIdOrderByIdAsc(project.getId());
        assertThat(saved).hasSize(1);
    }

    @Test
    @DisplayName("Duplicate name detection, explicit replacement, and snapshot immutability verification")
    void testDuplicateNameConflictAndSnapshotImmutability() throws Exception {
        // 1. Create existing library testcase
        TestCase originalCase = new TestCase();
        originalCase.setProjects(project);
        originalCase.setTestName("Duplicate Case Test");
        originalCase.setTestType(TestTypes.CUCUMBER_AUTOMATION);
        originalCase.setTestProjectId(project.getProjectInitials() + "-1");
        originalCase.setTestCreatedBy("Author");
        originalCase.setTestCreatedDate(new Timestamp(System.currentTimeMillis()));
        originalCase.setTestModifiedBy("Author");
        originalCase.setTestModifiedDate(new Timestamp(System.currentTimeMillis()));
        originalCase = testCaseRepository.save(originalCase);

        TestSteps oldStep1 = new TestSteps();
        oldStep1.setTestCase(originalCase);
        oldStep1.setTestType(TestTypes.CUCUMBER_AUTOMATION);
        oldStep1.setTestStepOrder(1);
        oldStep1.setTestStepDesc("Given open page \"https://old.url\"");
        oldStep1.setTestStepData("{\"order\":1,\"template\":\"OPEN_PAGE\",\"textValue\":\"https://old.url\"}");
        oldStep1.setTestExpectedOutput("");
        oldStep1.setTsCreatedBy("Author");
        oldStep1.setTsCreatedDate(Timestamp.valueOf("2026-01-01 10:00:00"));
        oldStep1.setTsModifiedBy("Author");
        oldStep1.setTsModifiedDate(Timestamp.valueOf("2026-01-01 10:00:00"));
        testStepsRepository.save(oldStep1);
        originalCase.setTestStepsSet(new HashSet<>(Set.of(oldStep1)));
        originalCase = testCaseRepository.save(originalCase);

        // 2. Create historical execution snapshot
        TestExecutions plan = new TestExecutions();
        plan.setExecutionName("Historical Plan");
        plan.setProjects(project);
        plan.setExecutionStatus(ExecutionStatus.CLOSED);
        plan = testPlanRepository.save(plan);

        String historicalSnapshot = "{\"caseId\":" + originalCase.getId() + ",\"steps\":[\"Given open page https://old.url\"]}";
        TestCaseExecutions link = new TestCaseExecutions();
        link.setTestExecutions(plan);
        link.setTestCase(originalCase);
        link.setCaseSnapshot(historicalSnapshot);
        link.setResultStatus(TestResult.PASS);
        link.setRecordedBy(manager.getUserName());
        link.setRecordedAt(java.time.Instant.now());
        link = linkRepository.save(link);

        // 3. Upload feature with same name
        String featureContent = """
                Feature: Test
                  Scenario: Duplicate Case Test
                    Given open page "https://new.updated.url"
                    When click element "login.submit"
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "dup.feature",
                "text/plain",
                featureContent.getBytes(StandardCharsets.UTF_8)
        );

        // Preview should detect conflict
        mvc.perform(multipart("/automation/projects/" + project.getId() + "/import/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cases[0].conflict").value(true))
                .andExpect(jsonPath("$.cases[0].existingCaseId").value(originalCase.getId()))
                .andExpect(jsonPath("$.cases[0].existingCaseName").value("Duplicate Case Test"))
                .andExpect(jsonPath("$.cases[0].action").value("CREATE_NEW")); // default is CREATE_NEW

        // 4. Save with explicit REPLACE_EXISTING
        List<AutomationStepModel> newSteps = List.of(
                AutomationStepModel.builder().order(1).keyword("Given").template(AutomationTemplate.OPEN_PAGE).textValue("https://new.updated.url").build(),
                AutomationStepModel.builder().order(2).keyword("When").template(AutomationTemplate.CLICK_ELEMENT).locatorType(LocatorType.NAMED).locatorValue("login.submit").build()
        );

        ImportSaveRequest replaceRequest = ImportSaveRequest.builder()
                .cases(List.of(ImportCaseSaveItem.builder()
                        .action(ImportAction.REPLACE_EXISTING)
                        .targetCaseId(originalCase.getId())
                        .caseName("Duplicate Case Test Updated")
                        .testType(TestTypes.CUCUMBER_AUTOMATION)
                        .steps(newSteps)
                        .build()))
                .build();

        mvc.perform(post("/automation/projects/" + project.getId() + "/import/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + managerToken)
                        .content(mapper.writeValueAsString(replaceRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replacedCount").value(1))
                .andExpect(jsonPath("$.createdCount").value(0));

        // 5. Verify library case was updated
        TestCase updatedCase = testCaseRepository.findById(originalCase.getId());
        assertThat(updatedCase.getTestName()).isEqualTo("Duplicate Case Test Updated");
        List<TestSteps> updatedSteps = testStepsRepository.findTestStepsByTestCaseAndTestTypeOrderByTestStepOrderAsc(updatedCase, TestTypes.CUCUMBER_AUTOMATION);
        assertThat(updatedSteps).hasSize(2);
        assertThat(updatedSteps.get(0).getTestStepDesc()).contains("https://new.updated.url");

        // 6. Verify historical execution snapshot remains UNCHANGED
        TestCaseExecutions freshLink = linkRepository.findById(link.getId()).orElseThrow();
        assertThat(freshLink.getCaseSnapshot()).isEqualTo(historicalSnapshot);
    }

    @Test
    @DisplayName("Cross-project access rejection: unauthorized user is rejected with 403 Forbidden")
    void testCrossProjectAccessRejection() throws Exception {
        String featureContent = """
                Feature: Test
                  Scenario: Unauthorized Scenario
                    Given open page "https://demo.app"
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.feature",
                "text/plain",
                featureContent.getBytes(StandardCharsets.UTF_8)
        );

        // Outsider attempts to preview on manager's project
        mvc.perform(multipart("/automation/projects/" + project.getId() + "/import/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());

        // Outsider attempts to save on manager's project
        ImportSaveRequest saveRequest = ImportSaveRequest.builder()
                .cases(List.of(ImportCaseSaveItem.builder()
                        .action(ImportAction.CREATE_NEW)
                        .caseName("Forbidden Case")
                        .testType(TestTypes.CUCUMBER_AUTOMATION)
                        .steps(List.of(AutomationStepModel.builder().order(1).template(AutomationTemplate.OPEN_PAGE).textValue("https://demo.app").build()))
                        .build()))
                .build();

        mvc.perform(post("/automation/projects/" + project.getId() + "/import/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + outsiderToken)
                        .content(mapper.writeValueAsString(saveRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Atomic rollback: if any case in a batch is invalid, all changes roll back")
    void testAtomicRollbackOnInvalidBatch() throws Exception {
        int initialCaseCount = testCaseRepository.findByProjectsIdOrderByIdAsc(project.getId()).size();

        // Valid case
        ImportCaseSaveItem validCase = ImportCaseSaveItem.builder()
                .action(ImportAction.CREATE_NEW)
                .caseName("Valid Case In Batch")
                .testType(TestTypes.CUCUMBER_AUTOMATION)
                .steps(List.of(AutomationStepModel.builder().order(1).template(AutomationTemplate.OPEN_PAGE).textValue("https://valid.url").build()))
                .build();

        // Invalid case (missing required locator)
        ImportCaseSaveItem invalidCase = ImportCaseSaveItem.builder()
                .action(ImportAction.CREATE_NEW)
                .caseName("Invalid Case In Batch")
                .testType(TestTypes.CUCUMBER_AUTOMATION)
                .steps(List.of(AutomationStepModel.builder().order(1).template(AutomationTemplate.CLICK_ELEMENT).locatorValue("").build()))
                .build();

        ImportSaveRequest batchRequest = ImportSaveRequest.builder()
                .cases(List.of(validCase, invalidCase))
                .build();

        mvc.perform(post("/automation/projects/" + project.getId() + "/import/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + managerToken)
                        .content(mapper.writeValueAsString(batchRequest)))
                .andExpect(status().isBadRequest());

        // Verify zero records were created
        List<TestCase> casesAfter = testCaseRepository.findByProjectsIdOrderByIdAsc(project.getId());
        assertThat(casesAfter).hasSize(initialCaseCount);
    }

    @Test
    @DisplayName("Secret masking: configured secrets use runtime references while raw preview text is masked")
    void testSecretReferencesInPreview() throws Exception {
        ProjectVariable secretVar = new ProjectVariable();
        secretVar.setProject(project);
        secretVar.setVariableKey("SUPER_SECRET_TOKEN");
        secretVar.setVariableValue("myUltraSecretValue123");
        secretVar.setIsSecret(true);
        variableRepository.save(secretVar);

        String featureWithSecret = """
                Feature: Secret Test
                  Scenario: Login with Secret
                    Given open page "https://demo.app"
                    When enter text "myUltraSecretValue123" into "login.user"
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "secret.feature",
                "text/plain",
                featureWithSecret.getBytes(StandardCharsets.UTF_8)
        );

        mvc.perform(multipart("/automation/projects/" + project.getId() + "/import/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cases[0].steps[1].textValue").value("${SUPER_SECRET_TOKEN}"))
                .andExpect(jsonPath("$.cases[0].steps[1].rawText").value(org.hamcrest.Matchers.containsString("[REDACTED]")))
                .andExpect(jsonPath("$.cases[0].steps[1].rawText").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("myUltraSecretValue123"))));
    }

    @Test
    @DisplayName("Export/Import round-trip preserves step definitions and supported case behavior")
    void testExportImportRoundTrip() throws Exception {
        // 1. Create a Cucumber case and a Keyword case in project
        TestCase cucumberCase = new TestCase();
        cucumberCase.setProjects(project);
        cucumberCase.setTestName("Roundtrip Cucumber");
        cucumberCase.setTestType(TestTypes.CUCUMBER_AUTOMATION);
        cucumberCase.setTestProjectId(project.getProjectInitials() + "-10");
        cucumberCase.setTestCreatedBy("QA");
        cucumberCase.setTestCreatedDate(new Timestamp(System.currentTimeMillis()));
        cucumberCase.setTestModifiedBy("QA");
        cucumberCase.setTestModifiedDate(new Timestamp(System.currentTimeMillis()));
        cucumberCase = testCaseRepository.save(cucumberCase);

        TestSteps s1 = new TestSteps();
        s1.setTestCase(cucumberCase);
        s1.setTestType(TestTypes.CUCUMBER_AUTOMATION);
        s1.setTestStepOrder(1);
        s1.setTestStepDesc("Given open page \"https://roundtrip.app\"");
        s1.setTestStepData("{\"order\":1,\"keyword\":\"Given\",\"template\":\"OPEN_PAGE\",\"textValue\":\"https://roundtrip.app\"}");
        s1.setTestExpectedOutput("");
        s1.setTsCreatedBy("QA");
        s1.setTsCreatedDate(new Timestamp(System.currentTimeMillis()));
        s1.setTsModifiedBy("QA");
        s1.setTsModifiedDate(new Timestamp(System.currentTimeMillis()));
        testStepsRepository.save(s1);

        TestSteps s2 = new TestSteps();
        s2.setTestCase(cucumberCase);
        s2.setTestType(TestTypes.CUCUMBER_AUTOMATION);
        s2.setTestStepOrder(2);
        s2.setTestStepDesc("When click element \"login.submit\"");
        s2.setTestStepData("{\"order\":2,\"keyword\":\"When\",\"template\":\"CLICK_ELEMENT\",\"locatorType\":\"NAMED\",\"locatorValue\":\"login.submit\"}");
        s2.setTestExpectedOutput("");
        s2.setTsCreatedBy("QA");
        s2.setTsCreatedDate(new Timestamp(System.currentTimeMillis()));
        s2.setTsModifiedBy("QA");
        s2.setTsModifiedDate(new Timestamp(System.currentTimeMillis()));
        testStepsRepository.save(s2);
        cucumberCase.setTestStepsSet(new HashSet<>(Set.of(s1, s2)));
        cucumberCase = testCaseRepository.save(cucumberCase);

        // 2. Export project to ZIP
        byte[] zipBytes = exportService.exportProjectZip(project.getId(), List.of(cucumberCase.getId()));
        assertThat(zipBytes).isNotEmpty();

        // 3. Extract the feature file from ZIP
        String extractedFeature = null;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().startsWith("src/test/resources/features/") && entry.getName().endsWith(".feature")) {
                    extractedFeature = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                    break;
                }
            }
        }
        assertThat(extractedFeature).isNotNull();

        // 4. Import the extracted feature file back into project with a different scenario name
        String roundtripFeature = extractedFeature.replace("Roundtrip Cucumber", "Imported Roundtrip Cucumber");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "extracted.feature",
                "text/plain",
                roundtripFeature.getBytes(StandardCharsets.UTF_8)
        );

        String previewJson = mvc.perform(multipart("/automation/projects/" + project.getId() + "/import/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasErrors").value(false))
                .andExpect(jsonPath("$.cases[0].steps.length()").value(2))
                .andExpect(jsonPath("$.cases[0].steps[0].template").value("OPEN_PAGE"))
                .andExpect(jsonPath("$.cases[0].steps[0].textValue").value("https://roundtrip.app"))
                .andExpect(jsonPath("$.cases[0].steps[1].template").value("CLICK_ELEMENT"))
                .andExpect(jsonPath("$.cases[0].steps[1].locatorValue").value("login.submit"))
                .andReturn().getResponse().getContentAsString();

        ImportPreviewResponse preview = mapper.readValue(previewJson, ImportPreviewResponse.class);
        ImportCasePreview cp = preview.getCases().get(0);

        List<AutomationStepModel> steps = cp.getSteps().stream().map(sp -> AutomationStepModel.builder()
                .order(sp.getOrder())
                .keyword(sp.getKeyword())
                .template(sp.getTemplate())
                .locatorType(sp.getLocatorType())
                .locatorValue(sp.getLocatorValue())
                .textValue(sp.getTextValue())
                .stateValue(sp.getStateValue())
                .build()).toList();

        mvc.perform(post("/automation/projects/" + project.getId() + "/import/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + managerToken)
                        .content(mapper.writeValueAsString(ImportSaveRequest.builder()
                                .cases(List.of(ImportCaseSaveItem.builder()
                                        .action(ImportAction.CREATE_NEW)
                                        .caseName(cp.getName())
                                        .testType(cp.getTestType())
                                        .steps(steps)
                                        .build()))
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(1));

        TestCase importedCase = testCaseRepository.findByProjectsIdOrderByIdAsc(project.getId()).stream()
                .filter(c -> c.getTestName().equals("Imported Roundtrip Cucumber"))
                .findFirst().orElseThrow();

        List<TestSteps> importedSteps = testStepsRepository.findTestStepsByTestCaseAndTestTypeOrderByTestStepOrderAsc(importedCase, TestTypes.CUCUMBER_AUTOMATION);
        assertThat(importedSteps).hasSize(2);
        assertThat(importedSteps.get(0).getTestStepDesc()).contains("https://roundtrip.app");
        assertThat(importedSteps.get(1).getTestStepDesc()).contains("login.submit");
    }
    private ImportPreviewResponse preview(String fileName, String content) throws Exception {
        String response=mvc.perform(multipart("/automation/projects/"+project.getId()+"/import/preview")
                        .file(new MockMultipartFile("file",fileName,"text/plain",content.getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization","Bearer "+managerToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readValue(response,ImportPreviewResponse.class);
    }

    private ImportSaveRequest confirmed(ImportPreviewResponse preview) {
        return ImportSaveRequest.builder().cases(preview.getCases().stream().map(item -> ImportCaseSaveItem.builder()
                .action(ImportAction.CREATE_NEW).caseName(item.getName()).testType(item.getTestType())
                .steps(item.getSteps().stream().map(step -> AutomationStepModel.builder().order(step.getOrder()).keyword(step.getKeyword())
                        .template(step.getTemplate()).locatorType(step.getLocatorType()).locatorValue(step.getLocatorValue())
                        .textValue(step.getTextValue()).stateValue(step.getStateValue()).build()).toList()).build()).toList()).build();
    }

    private void configureSecret(String key,String value) {
        ProjectVariable secret=new ProjectVariable(); secret.setProject(project); secret.setVariableKey(key); secret.setVariableValue(value); secret.setIsSecret(true);
        variableRepository.save(secret);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"feature","json"})
    void secretInputsSurvivePreviewSaveAndResolveAtRuntime(String format) throws Exception {
        String secret="private-import-password-123";
        configureSecret("PASSWORD",secret);
        configureSecret("SHORT_PASSWORD","password-123");
        String input=format.equals("feature")
                ? "Feature: Login\nScenario: Login\nWhen enter text \"prefix-"+secret+"-suffix ${PASSWORD}\" into \"#password\"\n"
                : "{\"name\":\"Login\",\"steps\":[{\"template\":\"ENTER_TEXT\",\"locatorValue\":\"#password\",\"textValue\":\"prefix-"+secret+"-suffix ${PASSWORD}\"}]}";
        var preview=preview("login."+format,input);
        assertThat(preview.isCanSave()).isTrue();
        assertThat(mapper.writeValueAsString(preview)).doesNotContain(secret);
        assertThat(preview.getCases().getFirst().getSteps().getFirst().getTextValue()).isEqualTo("prefix-${PASSWORD}-suffix ${PASSWORD}");
        var response=mvc.perform(post("/automation/projects/"+project.getId()+"/import/save")
                        .header("Authorization","Bearer "+managerToken).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(confirmed(preview))))
                .andExpect(status().isOk()).andReturn();
        var saved=mapper.readValue(response.getResponse().getContentAsString(),ImportSaveResponse.class);
        var testCase=testCaseRepository.findById(saved.getSavedCases().getFirst().getId());
        var stored=testStepsRepository.findTestStepsByTestCaseAndTestTypeOrderByTestStepOrderAsc(testCase,testCase.getTestType()).getFirst();
        var step=mapper.readValue(stored.getTestStepData(),AutomationStepModel.class);
        assertThat(stored.getTestStepData()).doesNotContain("[REDACTED]",secret);
        try (var driver=new AutomationBrowserDriver(AutomationTestDefinition.builder().variables(Map.of("PASSWORD",secret)).build())) {
            assertThat(driver.resolveVariables(step.getTextValue())).isEqualTo("prefix-"+secret+"-suffix "+secret);
        }
        assertThat(variableRepository.findByProjectIdAndVariableKey(project.getId(),"PASSWORD").orElseThrow().getVariableValue()).isEqualTo(secret);
    }

    @Test
    void ruleBackgroundsStayScopedAndFollowFeatureSetup() throws Exception {
        var result=preview("rules.feature", """
                Feature: Accounts
                  Background:
                    Given open page "https://example.test"
                  Scenario: Public page
                    When click element "#public"
                  Rule: Admin
                    Background:
                      Given enter text "admin" into "#username"
                    Scenario: Edit
                      When click element "#edit"
                    Scenario: Delete
                      When click element "#delete"
                  Rule: Guest
                    Background:
                      Given enter text "guest" into "#username"
                    Scenario: Read
                      When click element "#read"
                """);
        assertThat(result.isCanSave()).isTrue();
        assertThat(result.getCases()).hasSize(4);
        assertThat(result.getCases().get(0).getSteps()).hasSize(2);
        for (int index : List.of(1,2)) {
            var steps=result.getCases().get(index).getSteps();
            assertThat(steps).hasSize(3); assertThat(steps.get(0).getTextValue()).isEqualTo("https://example.test");
            assertThat(steps.get(1).getTextValue()).isEqualTo("admin");
            assertThat(steps).extracting(ImportStepPreview::getOrder).containsExactly(1,2,3);
        }
        assertThat(result.getCases().get(3).getSteps().get(1).getTextValue()).isEqualTo("guest");
        assertThat(result.getCases().get(1).getSteps().get(1).getSourceLine()).isEqualTo(8);
    }

    @Test
    void unsupportedRuleSetupCannotBeSilentlyDropped() throws Exception {
        var result=preview("rule-table.feature","Feature: Setup\nRule: Accounts\n Background:\n  Given enter text \"admin\" into \"#username\"\n   | role |\n   | admin |\n Scenario: Login\n  When click element \"#submit\"\n");
        assertThat(result.isCanSave()).isFalse();
        assertThat(result.getCases().getFirst().getSteps().getFirst().getUnsupportedSyntax()).contains("Data tables");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"feature","json"})
    void errorsAndMetadataNeverEchoConfiguredSecrets(String format) throws Exception {
        String secret="private-import-secret-789"; configureSecret("TOKEN",secret);
        String input=format.equals("json") ? "{\"name\":"+secret+"}" : "Feature: Broken\n  "+secret+"\n";
        var invalid=preview(secret+"."+format,input);
        assertThat(invalid.isCanSave()).isFalse(); assertThat(invalid.getGlobalErrors()).isNotEmpty();
        assertThat(mapper.writeValueAsString(invalid)).doesNotContain(secret);
        var metadata=preview(secret+".feature","Feature: "+secret+"\nScenario: "+secret+"\nGiven open page \"https://example.test\"\n");
        assertThat(mapper.writeValueAsString(metadata)).doesNotContain(secret);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={
        "{\"name\":\"Empty\",\"steps\":[]}",
        "{\"name\":\"Missing steps\"}",
        "{\"name\":\"   \",\"steps\":[{\"template\":\"OPEN_PAGE\",\"textValue\":\"https://example.test\"}]}",
        "{\"name\":\"Manual\",\"type\":\"MANUAL\",\"steps\":[{\"template\":\"OPEN_PAGE\",\"textValue\":\"https://example.test\"}]}",
        "{\"name\":\"Invalid\",\"type\":\"NOT_A_TYPE\",\"steps\":[{\"template\":\"OPEN_PAGE\",\"textValue\":\"https://example.test\"}]}"
    })
    void previewAndSaveAgreeOnInvalidCases(String input) throws Exception {
        var preview=preview("invalid.json",input);
        assertThat(preview.isCanSave()).isFalse(); assertThat(preview.isHasErrors()).isTrue();
        assertThat(preview.getCases().getFirst().getValidationError()).isNotBlank();
        mvc.perform(post("/automation/projects/"+project.getId()+"/import/save").header("Authorization","Bearer "+managerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(confirmed(preview))))
                .andExpect(status().isBadRequest());
        assertThat(testCaseRepository.findByProjectsIdOrderByIdAsc(project.getId())).isEmpty();
    }

    @Test
    void maskedValuesFromOldPreviewsAreRejectedInsteadOfPersisted() throws Exception {
        var request=confirmed(preview("old.feature","Feature: Old\nScenario: Old\nGiven open page \"https://example.test\"\n"));
        request.getCases().getFirst().getSteps().getFirst().setTextValue("[REDACTED]");
        mvc.perform(post("/automation/projects/"+project.getId()+"/import/save").header("Authorization","Bearer "+managerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        assertThat(testCaseRepository.findByProjectsIdOrderByIdAsc(project.getId())).isEmpty();
    }

}
