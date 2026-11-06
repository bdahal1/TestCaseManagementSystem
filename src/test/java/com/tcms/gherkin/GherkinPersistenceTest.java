package com.tcms.gherkin;

import com.tcms.dto.TestCaseInfoDTO;
import com.tcms.dto.plans.PlanRequests;
import com.tcms.enums.TestTypes;
import com.tcms.models.*;
import com.tcms.repositories.*;
import com.tcms.services.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class GherkinPersistenceTest {
    @Autowired TestCaseService caseService;
    @Autowired TestPlanService plans;
    @Autowired TestCaseRepository cases;
    @Autowired TestStepsRepository steps;
    @Autowired UserRepository users;
    @Autowired ProjectRepository projects;
    @Autowired PlatformTransactionManager transactions;
    int projectId,userId;
    String username;
    private static final String DOCUMENT="Scenario Outline: Sign in\n  Given I am on the sign in page\n  When I sign in as <user>\n  Then I should see <page>\n\n  Examples:\n    | user | page |\n    | standard-user | dashboard |\n";
    @BeforeEach void seed() {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            Projects project=new Projects(); project.setProjectName(UUID.randomUUID().toString()); project.setProjectInitials(UUID.randomUUID().toString()); projects.save(project); projectId=project.getId();
            Users user=new Users(); user.setUserName(UUID.randomUUID().toString()); user.setFirstName("QA"); user.setLastName("Editor"); user.setPassword("test-only");
            user.setProjectsSet(new HashSet<>(Set.of(project))); user.setRoleSet(new HashSet<>()); users.save(user); userId=user.getId(); username=user.getUserName();
        });
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(username,null,List.of(new SimpleGrantedAuthority("ROLE_QAManager"))));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    TestCaseInfoDTO request(String name,String document) {
        TestCaseInfoDTO dto=new TestCaseInfoDTO(); dto.setTestName(name); dto.setProjectId(projectId); dto.setUserId(userId); dto.setSelectedTags(new Integer[0]);
        dto.setTestType(TestTypes.CUCUMBER_MANUAL); dto.setGherkin(document); return dto;
    }
    int create(String document) { return ((TestCase)caseService.saveTestCase(request("Sign in",document)).getBody()).getId(); }
    String saved(int id) {
        return new TransactionTemplate(transactions).execute(status -> steps.findTestStepsByTestCaseAndTestTypeOrderByTestStepOrderAsc(cases.findById(id),TestTypes.CUCUMBER_MANUAL).getFirst().getTestStepDesc());
    }
    @Test void roundTripsALongDocumentWithoutAnExpectedResult() {
        String large=DOCUMENT+"    | "+"account".repeat(100)+" | dashboard |\n";
        int id=create(large);
        assertThat(saved(id)).isEqualTo(large);
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            var definitions=steps.findTestStepsByTestCaseAndTestTypeOrderByTestStepOrderAsc(cases.findById(id),TestTypes.CUCUMBER_MANUAL);
            assertThat(definitions).hasSize(1); assertThat(definitions.getFirst().getTestExpectedOutput()).isEmpty();
        });
    }
    @Test void invalidEditRollsBackBothMetadataAndScenario() {
        int id=create(DOCUMENT);
        assertThatThrownBy(() -> caseService.editTestCase(request("Changed title","Scenario Outline: Invalid\nGiven I am <user>"),id))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(cases.findById(id).getTestName()).isEqualTo("Sign in");
        assertThat(saved(id)).isEqualTo(DOCUMENT);
    }
    @Test void editingReplacesTheDefinitionButPreservesTheRunSnapshot() {
        int id=create(DOCUMENT);
        int plan=plans.create(new PlanRequests.Create(projectId,new PlanRequests.Metadata("Regression "+UUID.randomUUID(),null,null,null,null,null),List.of(id),userId));
        String changed="Given I am signed in\nWhen I open my account\nThen I see my details";
        caseService.editTestCase(request("Updated scenario",changed),id);
        assertThat(saved(id)).isEqualTo(changed);
        assertThat(plans.detail(plan).cases().getFirst().steps().getFirst().instruction()).isEqualTo(DOCUMENT);
        new TransactionTemplate(transactions).executeWithoutResult(status -> assertThat(steps.findTestStepsByTestCaseAndTestTypeOrderByTestStepOrderAsc(cases.findById(id),TestTypes.CUCUMBER_MANUAL)).hasSize(1));
    }
}
