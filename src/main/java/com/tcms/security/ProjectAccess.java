package com.tcms.security;

import com.tcms.dto.*;
import com.tcms.models.*;
import com.tcms.repositories.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Component("projectAccess")
@Transactional(readOnly = true)
public class ProjectAccess {
    private final UserRepository users;
    private final TestCaseRepository cases;
    private final TestExecutionRepository executions;
    private final TestFolderRepository folders;
    private final TestStepsRepository steps;

    public ProjectAccess(UserRepository users, TestCaseRepository cases, TestExecutionRepository executions,
                         TestFolderRepository folders, TestStepsRepository steps) {
        this.users = users; this.cases = cases; this.executions = executions;
        this.folders = folders; this.steps = steps;
    }
    public boolean manager() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_QAManager") || a.getAuthority().equals("ROLE_ADMIN"));
    }
    private Integer id(Object value) {
        try { return value == null ? null : Integer.valueOf(value.toString()); }
        catch (NumberFormatException e) { return null; }
    }
    public boolean project(Object value) {
        Integer projectId = id(value);
        if (projectId == null || projectId <= 0) return false;
        if (manager()) return true;
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        Users user = users.findByUserName(auth.getName());
        return user != null && Boolean.TRUE.equals(user.getIsActive()) && user.getProjectsSet() != null
                && user.getProjectsSet().stream().anyMatch(p -> projectId.equals(p.getId()));
    }
    private Projects caseProject(Object value) {
        Integer key = id(value);
        TestCase tc = key == null ? null : cases.findById(key.intValue());
        return tc == null ? null : tc.getProjects();
    }
    public boolean testCase(Object value) {
        Projects p = caseProject(value); return p != null && project(p.getId());
    }
    public boolean execution(Object value) {
        Integer key = id(value);
        TestExecutions e = key == null ? null : executions.findById(key.intValue());
        return e != null && e.getProjects() != null && project(e.getProjects().getId());
    }
    public boolean manageExecution(Object value) {
        if (!execution(value)) return false;
        TestExecutions plan=executions.findById(id(value).intValue());
        return !Boolean.TRUE.equals(plan.getReleasePlan()) || manager();
    }
    public boolean folder(Object value) {
        Integer key = id(value);
        TestFolders f = key == null ? null : folders.findById(key.intValue());
        return f != null && f.getProjects() != null && project(f.getProjects().getId());
    }
    public boolean step(Object value) {
        Integer key = id(value);
        TestSteps s = key == null ? null : steps.findById(key.intValue());
        return s != null && s.getTestCase() != null && testCase(s.getTestCase().getId());
    }
    public boolean ownUser(Object value) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        Users user = users.findByUserName(auth.getName());
        return manager() || user != null && Objects.equals(id(value), user.getId());
    }
    public boolean caseWrite(TestCaseInfoDTO dto) {
        return dto != null && project(dto.getProjectId());
    }
    public boolean stepWrite(TestStepInfoDTO dto) {
        return dto != null && testCase(dto.getTestCaseId())
                && (dto.getStepId() == null || dto.getStepId() == 0 || stepForCase(dto.getStepId(), dto.getTestCaseId()));
    }
    public boolean stepForCase(Object stepId, Object caseId) {
        Integer key = id(stepId);
        TestSteps s = key == null ? null : steps.findById(key.intValue());
        return s != null && s.getTestCase() != null && Objects.equals(s.getTestCase().getId(), id(caseId))
                && testCase(caseId);
    }
    public boolean stepWrites(List<TestStepInfoDTO> dtos) {
        return dtos != null && !dtos.isEmpty() && dtos.stream().allMatch(this::stepWrite)
                && dtos.stream().allMatch(d -> Objects.equals(dtos.get(0).getTestCaseId(), d.getTestCaseId()));
    }
    public boolean stepOrders(List<TestStepOrderDTO> dtos) {
        if (dtos == null || dtos.isEmpty() || dtos.stream().anyMatch(d -> d == null || d.getTestStepOrder() == null
                || d.getTestStepOrder() < 1 || !step(d.getTestStepId()))) return false;
        TestSteps first = steps.findById(dtos.get(0).getTestStepId().intValue());
        return dtos.stream().allMatch(d -> {
            TestSteps current = steps.findById(d.getTestStepId().intValue());
            return Objects.equals(first.getTestCase().getId(), current.getTestCase().getId())
                    && first.getTestType() == current.getTestType();
        });
    }
    public boolean result(TestResultRequestDTO dto) {
        return dto != null && dto.getResultStatus() != null && execution(dto.getExecutionId()) && testCase(dto.getTestCaseId());
    }
    public boolean links(Object value, Map<String, List<Integer>> body, boolean isExecution) {
        Integer key = id(value);
        if (key == null || body == null || body.get("testCaseIds") == null) return false;
        Projects target;
        if (isExecution) {
            if (!execution(key)) return false;
            target = executions.findById(key.intValue()).getProjects();
        } else {
            if (!folder(key)) return false;
            target = folders.findById(key.intValue()).getProjects();
        }
        return body.get("testCaseIds").stream().allMatch(caseId -> {
            Projects p = caseProject(caseId);
            return p != null && Objects.equals(p.getId(), target.getId());
        });
    }
}
