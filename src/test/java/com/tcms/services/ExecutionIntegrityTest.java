package com.tcms.services;

import com.tcms.controller.TestCaseController;
import com.tcms.models.*;
import com.tcms.repositories.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.sql.Timestamp;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class ExecutionIntegrityTest {
    @Autowired ProjectRepository projects;
    @Autowired TestCaseRepository cases;
    @Autowired TestExecutionRepository executions;
    @Autowired TestExecutionService service;
    @Autowired TestCaseController controller;
    @Autowired PlatformTransactionManager transactions;
    int executionId, firstId, foreignId;

    @BeforeEach void seed() {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            Projects p = project(); Projects other = project();
            TestExecutions execution = new TestExecutions(); execution.setExecutionName("run-" + UUID.randomUUID()); execution.setProjects(p);
            executionId = executions.save(execution).getId();
            firstId = testCase(p).getId(); foreignId = testCase(other).getId();
        });
    }
    Projects project() {
        Projects p = new Projects(); p.setProjectName(UUID.randomUUID().toString()); p.setProjectInitials(UUID.randomUUID().toString());
        return projects.save(p);
    }
    TestCase testCase(Projects project) {
        TestCase tc = new TestCase(); tc.setProjects(project); tc.setTestName("case"); tc.setTestProjectId(UUID.randomUUID().toString());
        tc.setTestCreatedBy("tester"); tc.setTestModifiedBy("tester");
        tc.setTestCreatedDate(new Timestamp(System.currentTimeMillis())); tc.setTestModifiedDate(tc.getTestCreatedDate());
        return cases.save(tc);
    }
    int links() {
        return new TransactionTemplate(transactions).execute(status -> executions.findById(executionId).getTestCaseExecutions().size());
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void mixedProjectBatchRollsBack() {
        assertThatThrownBy(() -> service.addCases(executionId, List.of(firstId, foreignId))).isInstanceOf(IllegalArgumentException.class);
        assertThat(links()).isZero();
    }
    @Test void duplicateAddsAreIdempotentAndRemovalDeletesTheLink() {
        service.addCases(executionId, List.of(firstId, firstId));
        service.addCases(executionId, List.of(firstId));
        assertThat(links()).isEqualTo(1);
        service.removeCases(executionId, List.of(firstId));
        assertThat(links()).isZero();
    }
    @Test void controllerMethodSecurityRejectsUnassignedUser() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("outsider", null, List.of()));
        assertThatThrownBy(() -> controller.getTestCaseById(firstId)).isInstanceOf(AccessDeniedException.class);
    }

    @Test void httpValidationAndPermissionFailuresHavePublicErrors() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("outsider", null, List.of()));
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new com.tcms.errors.ApiExceptionHandler()).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/testCase")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message").value("Bad Request"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/testCase/id/" + firstId))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message").value("Forbidden"));
    }
}
