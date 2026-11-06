package com.tcms.security;

import com.tcms.dto.TestStepInfoDTO;
import com.tcms.models.*;
import com.tcms.repositories.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectAccessTest {
    UserRepository users = mock(UserRepository.class);
    TestCaseRepository cases = mock(TestCaseRepository.class);
    TestExecutionRepository executions = mock(TestExecutionRepository.class);
    TestFolderRepository folders = mock(TestFolderRepository.class);
    TestStepsRepository steps = mock(TestStepsRepository.class);
    ProjectAccess access = new ProjectAccess(users, cases, executions, folders, steps);
    Projects assigned = project(1), other = project(2);
    private static Projects project(int id) { Projects p = new Projects(); p.setId(id); return p; }
    @BeforeEach void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("engineer", null, List.of()));
        Users user = new Users(); user.setProjectsSet(Set.of(assigned)); user.setId(7);
        when(users.findByUserName("engineer")).thenReturn(user);
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void onlyAssignedProjectsAreAccessible() {
        assertThat(access.project(1)).isTrue();
        assertThat(access.project(2)).isFalse();
        assertThat(access.project("invalid")).isFalse();
        assertThat(access.ownUser(7)).isTrue();
        assertThat(access.ownUser(8)).isFalse();
    }
    @Test void resourceIdDoesNotBypassMembership() {
        TestCase tc = new TestCase(); tc.setProjects(other);
        when(cases.findById(9)).thenReturn(tc);
        assertThat(access.testCase(9)).isFalse();
    }
    @Test void managerCannotLinkCasesFromAnotherProject() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("manager", null,
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_QAManager"))));
        TestExecutions execution = new TestExecutions(); execution.setProjects(assigned);
        when(executions.findById(10)).thenReturn(execution);
        TestCase tc = new TestCase(); tc.setProjects(other);
        when(cases.findById(9)).thenReturn(tc);
        assertThat(access.links(10, Map.of("testCaseIds", List.of(9)), true)).isFalse();
    }
    @Test void batchCannotReuseStepFromAnotherCase() {
        TestCase first = new TestCase(); first.setId(3); first.setProjects(assigned);
        TestCase second = new TestCase(); second.setId(4); second.setProjects(assigned);
        when(cases.findById(3)).thenReturn(first);
        TestSteps step = new TestSteps(); step.setTestCase(second);
        when(steps.findById(5)).thenReturn(step);
        TestStepInfoDTO dto = new TestStepInfoDTO(); dto.setTestCaseId(3); dto.setStepId(5);
        assertThat(access.stepWrites(List.of(dto))).isFalse();
        assertThat(access.stepWrites(List.of())).isFalse();
    }
    @Test void anonymousAndInactiveUsersAreDenied() {
        users.findByUserName("engineer").setIsActive(false);
        assertThat(access.project(1)).isFalse();
        SecurityContextHolder.clearContext();
        assertThat(access.project(1)).isFalse();
    }
}
