package com.tcms.plans;

import tools.jackson.databind.*;
import com.tcms.authentication.config.JwtTokenUtil;
import com.tcms.authentication.service.CustomUserDetailService;
import com.tcms.dto.plans.*;
import com.tcms.enums.*;
import com.tcms.models.*;
import com.tcms.repositories.*;
import com.tcms.services.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class TestPlanWorkflowTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ProjectRepository projects;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired TestCaseRepository cases;
    @Autowired TestPlanService plans;
    @Autowired TestExecutionService executions;
    @Autowired JwtTokenUtil tokens;
    @Autowired CustomUserDetailService details;
    Projects project, foreign;
    Users manager, tester, colleague, outsider;
    TestCase tc, another;
    String managerToken, testerToken, colleagueToken, outsiderToken;

    @BeforeEach void seed() {
        project=project(); foreign=project();
        Roles managerRole=new Roles(); managerRole.setRoleName("ROLE_QAManager"); managerRole=roles.save(managerRole);
        Roles engineerRole=new Roles(); engineerRole.setRoleName("ROLE_QAEngineer"); engineerRole=roles.save(engineerRole);
        manager=user(project,managerRole); tester=user(project,engineerRole); colleague=user(project,engineerRole); outsider=user(foreign,engineerRole);
        tc=testCase(project); another=testCase(foreign);
        managerToken=token(manager); testerToken=token(tester); colleagueToken=token(colleague); outsiderToken=token(outsider);
        asManager();
    }
    Projects project() {
        Projects p=new Projects(); p.setProjectName(UUID.randomUUID().toString()); p.setProjectInitials(UUID.randomUUID().toString()); return projects.save(p);
    }
    Users user(Projects project,Roles role) {
        Users u=new Users(); u.setUserName(UUID.randomUUID().toString()); u.setFirstName("QA"); u.setLastName("Tester"); u.setPassword("unused-hash");
        u.setRoleSet(new HashSet<>(Set.of(role))); u.setProjectsSet(new HashSet<>(Set.of(project))); return users.save(u);
    }
    TestCase testCase(Projects p) {
        TestCase c=new TestCase(); c.setProjects(p); c.setTestName("Login with valid credentials"); c.setTestProjectId(UUID.randomUUID().toString());
        c.setTestCreatedBy("QA"); c.setTestModifiedBy("QA"); c.setTestCreatedDate(new Timestamp(System.currentTimeMillis())); c.setTestModifiedDate(c.getTestCreatedDate());
        TestSteps step=new TestSteps(); step.setTestStepDesc("Enter valid credentials"); step.setTestExpectedOutput("Dashboard opens"); step.setTestStepOrder(1);
        step.setTsCreatedBy("QA"); step.setTsModifiedBy("QA"); step.setTsCreatedDate(c.getTestCreatedDate()); step.setTsModifiedDate(c.getTestCreatedDate());
        step.setTestCase(c); c.setTestStepsSet(new HashSet<>(Set.of(step))); return cases.save(c);
    }
    String token(Users user) { return "Bearer "+tokens.generateToken(details.loadUserByUsername(user.getUserName())); }
    void asManager() { SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(manager.getUserName(),null,List.of(new SimpleGrantedAuthority("ROLE_QAManager")))); }
    int create(boolean assigned) {
        asManager();
        return plans.create(new PlanRequests.Create(project.getId(),new PlanRequests.Metadata("Release "+UUID.randomUUID(),"2.4","rc.1","Staging",null,"Regression"),List.of(tc.getId()),assigned ? tester.getId() : null));
    }
    int active() { int id=create(true); plans.transition(id,"start"); return id; }
    String result(TestResult status,long revision) throws Exception { return json.writeValueAsString(new PlanRequests.Result(status,"Notes","Observed behavior","https://tracker.example/BUG-1",revision)); }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void managerCreatesDraftWithReleaseMetadataAndAssignment() throws Exception {
        var request=new PlanRequests.Create(project.getId(),new PlanRequests.Metadata("Release regression","2.4","rc.1","Staging",java.time.LocalDate.of(2026,11,9),"Core journeys"),List.of(tc.getId()),tester.getId());
        var response=mvc.perform(post("/testPlans").header("Authorization",managerToken).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
                .andExpect(status().isCreated()).andReturn();
        int id=json.readTree(response.getResponse().getContentAsString()).get("id").asInt();
        mvc.perform(get("/testPlans/"+id).header("Authorization",testerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.plan.status").value("DRAFT"))
                .andExpect(jsonPath("$.plan.build").value("rc.1")).andExpect(jsonPath("$.plan.dueDate").value("2026-11-09")).andExpect(jsonPath("$.cases[0].assignee.id").value(tester.getId()))
                .andExpect(jsonPath("$.cases[0].canRecord").value(false));
    }
    @Test void draftRequiresAssignmentsAndActiveTesterMembership() {
        int id=create(false);
        assertThatThrownBy(() -> plans.transition(id,"start")).hasMessageContaining("Assign every case");
    }
    @Test void assignmentRejectsUserFromAnotherProject() throws Exception {
        int id=create(false);
        mvc.perform(put("/testPlans/"+id+"/assignments").header("Authorization",managerToken).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new PlanRequests.Assignment(List.of(tc.getId()),outsider.getId()))))
                .andExpect(status().isBadRequest());
    }
    @Test void engineerCannotCreateOrAssignPlansAndOutsiderCannotRead() throws Exception {
        int id=create(true);
        mvc.perform(put("/testPlans/"+id+"/assignments").header("Authorization",testerToken).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new PlanRequests.Assignment(List.of(tc.getId()),colleague.getId())))).andExpect(status().isForbidden());
        mvc.perform(get("/testPlans/"+id).header("Authorization",outsiderToken)).andExpect(status().isForbidden());
    }
    @Test void onlyAssignedTesterOrManagerCanRecord() throws Exception {
        int id=active();
        mvc.perform(post("/testPlans/"+id+"/cases/"+tc.getId()+"/results").header("Authorization",colleagueToken)
                .contentType(MediaType.APPLICATION_JSON).content(result(TestResult.PASS,0))).andExpect(status().isForbidden());
        mvc.perform(post("/testPlans/"+id+"/cases/"+tc.getId()+"/results").header("Authorization",testerToken)
                .contentType(MediaType.APPLICATION_JSON).content(result(TestResult.PASS,0))).andExpect(status().isNoContent());
        mvc.perform(get("/testPlans/"+id).header("Authorization",managerToken)).andExpect(jsonPath("$.plan.completed").value(1))
                .andExpect(jsonPath("$.cases[0].history[0].status").value("PASS")).andExpect(jsonPath("$.cases[0].revision").value(1));
    }
    @Test void staleResultsReturnConflictWithoutOverwriting() throws Exception {
        int id=active();
        mvc.perform(post("/testPlans/"+id+"/cases/"+tc.getId()+"/results").header("Authorization",testerToken)
                .contentType(MediaType.APPLICATION_JSON).content(result(TestResult.FAIL,0))).andExpect(status().isNoContent());
        mvc.perform(post("/testPlans/"+id+"/cases/"+tc.getId()+"/results").header("Authorization",testerToken)
                .contentType(MediaType.APPLICATION_JSON).content(result(TestResult.PASS,0))).andExpect(status().isConflict());
        mvc.perform(get("/testPlans/"+id).header("Authorization",managerToken)).andExpect(jsonPath("$.cases[0].status").value("FAIL"));
    }
    @Test void snapshotsPreserveNameAndStepsAfterLibraryEdits() throws Exception {
        int id=create(true);
        tc.setTestName("Renamed case"); tc.getTestStepsSet().iterator().next().setTestStepDesc("Changed steps"); cases.save(tc);
        mvc.perform(get("/testPlans/"+id).header("Authorization",managerToken))
                .andExpect(jsonPath("$.cases[0].name").value("Login with valid credentials"))
                .andExpect(jsonPath("$.cases[0].steps[0].instruction").value("Enter valid credentials"));
    }
    @Test void blockedPlanCannotCloseAndClosedResultsCannotChange() throws Exception {
        int id=active();
        mvc.perform(post("/testPlans/"+id+"/actions/close").header("Authorization",managerToken)).andExpect(status().isConflict());
        mvc.perform(post("/testPlans/"+id+"/cases/"+tc.getId()+"/results").header("Authorization",testerToken)
                .contentType(MediaType.APPLICATION_JSON).content(result(TestResult.FAIL,0))).andExpect(status().isNoContent());
        mvc.perform(post("/testPlans/"+id+"/actions/close").header("Authorization",managerToken)).andExpect(status().isNoContent());
        mvc.perform(post("/testPlans/"+id+"/cases/"+tc.getId()+"/results").header("Authorization",testerToken)
                .contentType(MediaType.APPLICATION_JSON).content(result(TestResult.PASS,1))).andExpect(status().isConflict());
        mvc.perform(post("/testPlans/"+id+"/actions/reopen").header("Authorization",managerToken)).andExpect(status().isNoContent());
        mvc.perform(post("/testPlans/"+id+"/cases/"+tc.getId()+"/results").header("Authorization",testerToken)
                .contentType(MediaType.APPLICATION_JSON).content(result(TestResult.PASS,1))).andExpect(status().isNoContent());
        mvc.perform(get("/testPlans/"+id).header("Authorization",managerToken)).andExpect(jsonPath("$.cases[0].history.length()").value(2));
    }
    @Test void evidenceUploadDownloadAndWrongPlanProtection() throws Exception {
        int id=active();
        MockMultipartFile file=new MockMultipartFile("file","failure.log","text/plain","Request failed: expected 200, got 500".getBytes());
        var response=mvc.perform(multipart("/testPlans/"+id+"/cases/"+tc.getId()+"/evidence").file(file).header("Authorization",testerToken))
                .andExpect(status().isCreated()).andReturn();
        int fileId=json.readTree(response.getResponse().getContentAsString()).get("id").asInt();
        mvc.perform(get("/testPlans/"+id+"/evidence/"+fileId).header("Authorization",testerToken)).andExpect(status().isOk())
                .andExpect(content().bytes(file.getBytes())).andExpect(header().string("X-Content-Type-Options","nosniff"));
        int otherPlan=create(true);
        mvc.perform(get("/testPlans/"+otherPlan+"/evidence/"+fileId).header("Authorization",testerToken)).andExpect(status().isNotFound());
    }
    @Test void invalidFilesAndUnassignedEvidenceAreRejected() throws Exception {
        int id=active();
        var file=new MockMultipartFile("file","attack.html","text/html","<script>alert(1)</script>".getBytes());
        mvc.perform(multipart("/testPlans/"+id+"/cases/"+tc.getId()+"/evidence").file(file).header("Authorization",testerToken)).andExpect(status().isBadRequest());
        var log=new MockMultipartFile("file","failure.txt","text/plain","Log".getBytes());
        mvc.perform(multipart("/testPlans/"+id+"/cases/"+tc.getId()+"/evidence").file(log).header("Authorization",colleagueToken)).andExpect(status().isForbidden());
    }
    @Test void recordedCasesCannotBeRemovedAndOldResultEndpointCannotBypassRevision() throws Exception {
        int id=active();
        mvc.perform(post("/testExecutions/results").header("Authorization",testerToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"executionId\":"+id+",\"testCaseId\":"+tc.getId()+",\"resultStatus\":\"PASS\"}")).andExpect(status().isConflict());
        mvc.perform(post("/testPlans/"+id+"/cases/"+tc.getId()+"/results").header("Authorization",testerToken)
                .contentType(MediaType.APPLICATION_JSON).content(result(TestResult.PASS,0))).andExpect(status().isNoContent());
        mvc.perform(delete("/testPlans/"+id+"/cases/"+tc.getId()).header("Authorization",managerToken)).andExpect(status().isConflict());
        mvc.perform(delete("/testCase/"+tc.getId()).header("Authorization",managerToken)).andExpect(status().isConflict());
    }

    @Test void assignmentAndSearchFiltersApplyBeforePagination() throws Exception {
        int assigned=create(true); create(false);
        mvc.perform(get("/testPlans/project/"+project.getId()).param("scope","mine").param("size","1").header("Authorization",testerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.plans[0].id").value(assigned));
        mvc.perform(get("/testPlans/project/"+project.getId()).param("q","does not exist").header("Authorization",testerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(0));
    }
    @Test void nonHttpDefectUrlsAndMissingFailureContextAreRejected() throws Exception {
        int id=active();
        var unsafe=new PlanRequests.Result(TestResult.FAIL,"Reason","Observed","javascript:alert(1)",0L);
        mvc.perform(post("/testPlans/"+id+"/cases/"+tc.getId()+"/results").header("Authorization",testerToken)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(unsafe))).andExpect(status().isBadRequest());
        var empty=new PlanRequests.Result(TestResult.BLOCKED,"","",null,0L);
        mvc.perform(post("/testPlans/"+id+"/cases/"+tc.getId()+"/results").header("Authorization",testerToken)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(empty))).andExpect(status().isBadRequest());
    }

    @Test void loginAndRefreshUseTheMigratedJsonAndSecurityStack() throws Exception {
        tester.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("workflow-test-password")); users.save(tester);
        String body=json.writeValueAsString(Map.of("userName",tester.getUserName(),"password","workflow-test-password"));
        var response=mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.userId").value(tester.getId())).andReturn();
        String refresh=json.readTree(response.getResponse().getContentAsString()).get("data").get("refreshToken").asText();
        mvc.perform(post("/refreshToken").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("refreshToken",refresh))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }
    @Test void disabledUserCannotSignInAfterMigration() throws Exception {
        tester.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("workflow-test-password")); tester.setIsActive(false); users.save(tester);
        mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("userName",tester.getUserName(),"password","workflow-test-password"))))
                .andExpect(status().isUnauthorized());
    }

    @Test void gherkinSuggestionsAreScopedToProjectMembership() throws Exception {
        tc.setTestType(TestTypes.CUCUMBER_MANUAL); tc.getTestStepsSet().iterator().next().setTestType(TestTypes.CUCUMBER_MANUAL);
        tc.getTestStepsSet().iterator().next().setTestStepDesc("Given I am signed out\nWhen I sign in\nThen I see my dashboard"); cases.save(tc);
        mvc.perform(get("/testCase/gherkin-suggestions").param("projectId",project.getId().toString()).header("Authorization",testerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0]").value("Given I am signed out"));
        mvc.perform(get("/testCase/gherkin-suggestions").param("projectId",project.getId().toString()).header("Authorization",outsiderToken))
                .andExpect(status().isForbidden());
    }
}
