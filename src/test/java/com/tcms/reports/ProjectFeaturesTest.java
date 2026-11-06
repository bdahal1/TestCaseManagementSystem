package com.tcms.reports;

import com.tcms.authentication.config.JwtTokenUtil;
import com.tcms.authentication.service.CustomUserDetailService;
import com.tcms.dto.TestCaseInfoDTO;
import com.tcms.dto.plans.PlanRequests;
import com.tcms.enums.*;
import com.tcms.models.*;
import com.tcms.repositories.*;
import com.tcms.services.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjectFeaturesTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired TestCaseService caseService;
    @Autowired TestPlanService plans;
    @Autowired ProjectReports reports;
    @Autowired TestCaseRepository cases;
    @Autowired TestCaseExecutionRepository links;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired ProjectRepository projects;
    @Autowired JwtTokenUtil jwt;
    @Autowired CustomUserDetailService userDetails;
    Projects project, foreign;
    Users manager, tester, outsider;
    String managerToken, testerToken, outsiderToken;
    @BeforeEach void seed() {
        project=project(); foreign=project();
        Roles managerRole=new Roles(); managerRole.setRoleName("ROLE_QAManager"); roles.save(managerRole);
        Roles qa=new Roles(); qa.setRoleName("ROLE_QAEngineer"); roles.save(qa);
        manager=user(project,managerRole); tester=user(project,qa); outsider=user(foreign,qa);
        managerToken=token(manager); testerToken=token(tester); outsiderToken=token(outsider);
        asManager();
    }
    Projects project() { Projects p=new Projects(); p.setProjectName(UUID.randomUUID().toString()); p.setProjectInitials("QA"+UUID.randomUUID()); return projects.save(p); }
    Users user(Projects p,Roles role) {
        Users user=new Users(); user.setUserName(UUID.randomUUID().toString()); user.setFirstName("QA"); user.setLastName("Tester"); user.setPassword("test-only");
        user.setRoleSet(new HashSet<>(Set.of(role))); user.setProjectsSet(new HashSet<>(Set.of(p))); return users.save(user);
    }
    String token(Users user) { return "Bearer "+jwt.generateToken(userDetails.loadUserByUsername(user.getUserName())); }
    void asManager() { SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(manager.getUserName(),null,List.of(new SimpleGrantedAuthority("ROLE_QAManager")))); }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    TestCaseInfoDTO request() {
        TestCaseInfoDTO dto=new TestCaseInfoDTO(); dto.setProjectId(project.getId()); dto.setUserId(manager.getId()); dto.setTestName("Sign in"); dto.setTestType(TestTypes.MANUAL);
        dto.setPriority(CasePriority.CRITICAL); dto.setAutomationStatus(AutomationStatus.PLANNED); dto.setComponent("Authentication"); dto.setOwnerId(tester.getId());
        dto.setEstimatedMinutes(8); dto.setDescription("Verify account access"); dto.setPreconditions("An active staging account"); dto.setCustomFields(Map.of("Requirement","AUTH-12"));
        return dto;
    }
    int create() { return ((TestCase)caseService.saveTestCase(request()).getBody()).getId(); }
    int plan(int caseId,String environment,String release,boolean start) {
        asManager();
        int id=plans.create(new PlanRequests.Create(project.getId(),new PlanRequests.Metadata(UUID.randomUUID().toString(),release,"rc.1",environment,null,null),List.of(caseId),tester.getId()));
        if (start) plans.transition(id,"start"); return id;
    }
    void record(int plan,int tc,TestResult result,long revision) { asManager(); plans.record(plan,tc,new PlanRequests.Result(result,"Notes","Observed", "https://bugs.example/AUTH-1",revision)); }

    @Test void persistsAndSerializesDetailsAndAllowsClearingThem() throws Exception {
        var response=mvc.perform(post("/testCase").header("Authorization",managerToken).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.priority").value("CRITICAL"))
                .andExpect(jsonPath("$.customFields.Requirement").value("AUTH-12")).andReturn();
        int id=json.readTree(response.getResponse().getContentAsString()).get("id").asInt();
        mvc.perform(put("/testCase/"+id).header("Authorization",managerToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"ownerId\":0,\"estimatedMinutes\":0,\"preconditions\":\"\",\"customFields\":{}}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ownerId").isEmpty()).andExpect(jsonPath("$.estimatedMinutes").isEmpty())
                .andExpect(jsonPath("$.customFields").isEmpty()).andExpect(jsonPath("$.priority").value("CRITICAL"));
    }
    @Test void validatesDetailsAndRejectsAnOwnerFromAnotherProject() throws Exception {
        var dto=request(); dto.setOwnerId(outsider.getId());
        mvc.perform(post("/testCase").header("Authorization",managerToken).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dto))).andExpect(status().isBadRequest());
        dto=request(); dto.setEstimatedMinutes(-1);
        mvc.perform(post("/testCase").header("Authorization",managerToken).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dto))).andExpect(status().isBadRequest());
        dto=request(); dto.setCustomFields(Map.of("  ","value"));
        mvc.perform(post("/testCase").header("Authorization",managerToken).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dto))).andExpect(status().isBadRequest());
        var duplicate=new LinkedHashMap<String,String>(); duplicate.put("Ticket","A"); duplicate.put(" Ticket ","B"); dto=request(); dto.setCustomFields(duplicate);
        mvc.perform(post("/testCase").header("Authorization",managerToken).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dto))).andExpect(status().isBadRequest());
    }
    @Test void snapshotsPreserveSetupAndEstimatesAfterLibraryEdits() {
        int tc=create(); int plan=plan(tc,"Staging","2.4",true);
        var edit=request(); edit.setPreconditions("Changed setup"); edit.setEstimatedMinutes(99); edit.setCustomFields(Map.of("Requirement","NEW")); caseService.editTestCase(edit,tc);
        var saved=plans.detail(plan).cases().getFirst().details();
        assertThat(saved.preconditions()).isEqualTo("An active staging account"); assertThat(saved.estimatedMinutes()).isEqualTo(8);
        assertThat(saved.customFields()).containsEntry("Requirement","AUTH-12");
        assertThat(reports.report(project.getId(),null,null,"","").workload().getFirst().estimatedRemainingMinutes()).isEqualTo(8);
        record(plan,tc,TestResult.PASS,0);
        assertThat(reports.report(project.getId(),null,null,"","").cases().getFirst().definitionChanged()).isTrue();
    }
    @Test void legacySnapshotsRemainReadableWithoutDetails() {
        int tc=create(); int plan=plan(tc,"Staging","2.4",true);
        links.findByTestExecutionsIdAndTestCaseId(plan,tc).orElseThrow().setCaseSnapshot("{\"name\":\"Legacy case\",\"type\":\"MANUAL\",\"steps\":[]}");
        assertThat(plans.detail(plan).cases().getFirst().details()).isNull();
        assertThat(reports.report(project.getId(),null,null,"","").workload().getFirst().missingEstimates()).isEqualTo(1);
    }
    @Test void countsExecutionMembershipsSeparatelyFromUniqueCasesAndAttempts() {
        int tc=create(); int one=plan(tc,"Staging","2.4",true); int two=plan(tc,"QA","2.5",true); plan(tc,"QA","2.5",false);
        record(one,tc,TestResult.FAIL,0); record(one,tc,TestResult.PASS,1); record(two,tc,TestResult.BLOCKED,0);
        var report=reports.report(project.getId(),null,null,"","");
        assertThat(report.library().total()).isEqualTo(1); assertThat(report.execution().total()).isEqualTo(2);
        assertThat(report.execution().testedCases()).isEqualTo(1); assertThat(report.execution().draftPlans()).isEqualTo(1);
        assertThat(report.execution().passRate()).isEqualTo(100.0); assertThat(report.execution().completionRate()).isEqualTo(50.0);
        assertThat(report.execution().linkedDefects()).isEqualTo(1);
        assertThat(report.activity().stream().flatMap(d -> d.results().values().stream()).mapToLong(Long::longValue).sum()).isEqualTo(3);
        assertThat(report.cases().getFirst().result()).isEqualTo(TestResult.BLOCKED);
        var staging=reports.report(project.getId(),null,null,"Staging","2.4");
        assertThat(staging.execution().total()).isEqualTo(1); assertThat(staging.cases().getFirst().result()).isEqualTo(TestResult.PASS);
        assertThat(staging.library().total()).isEqualTo(1);
    }
    @Test void activityDatesUseInclusiveUtcDaysAndDoNotFilterCurrentTotals() {
        int tc=create(); int plan=plan(tc,"Staging","2.4",true); record(plan,tc,TestResult.PASS,0);
        var link=links.findByTestExecutionsIdAndTestCaseId(plan,tc).orElseThrow();
        link.getHistory().getFirst().setRecordedAt(Instant.parse("2026-01-02T23:59:59Z"));
        record(plan,tc,TestResult.FAIL,1); link.getHistory().getLast().setRecordedAt(Instant.parse("2026-01-03T00:00:00Z"));
        var report=reports.report(project.getId(),LocalDate.of(2026,1,2),LocalDate.of(2026,1,2),"","");
        assertThat(report.activity()).hasSize(1); assertThat(report.activity().getFirst().results().get(TestResult.PASS)).isEqualTo(1);
        assertThat(report.activity().getFirst().results().get(TestResult.FAIL)).isZero(); assertThat(report.execution().results().get(TestResult.FAIL)).isEqualTo(1);
    }
    @Test void emptyReportsDoNotInventRatesAndResetIsTheLatestOutcome() {
        var empty=reports.report(project.getId(),null,null,"",""); assertThat(empty.execution().passRate()).isNull(); assertThat(empty.execution().completionRate()).isNull();
        int tc=create(); int plan=plan(tc,"Staging","2.4",true); record(plan,tc,TestResult.PASS,0); record(plan,tc,TestResult.NOT_RUN,1);
        var report=reports.report(project.getId(),null,null,"","");
        assertThat(report.cases().getFirst().result()).isEqualTo(TestResult.NOT_RUN); assertThat(report.execution().testedCases()).isZero(); assertThat(report.execution().neverRunCases()).isEqualTo(1);
    }
    @Test void enforcesProjectAccessAndValidActivityRanges() throws Exception {
        create();
        mvc.perform(get("/projectReports/"+project.getId()).header("Authorization",testerToken)).andExpect(status().isOk()).andExpect(jsonPath("$.library.total").value(1));
        mvc.perform(get("/projectReports/"+project.getId()).header("Authorization",outsiderToken)).andExpect(status().isForbidden());
        mvc.perform(get("/projectReports/"+project.getId()).header("Authorization",testerToken).param("from","2026-11-02").param("to","2026-11-01")).andExpect(status().isBadRequest());
        mvc.perform(get("/projectReports/"+project.getId()).header("Authorization",testerToken).param("from","2020-01-01")).andExpect(status().isBadRequest());
    }
}
