package com.tcms.services;

import com.tcms.models.*;
import com.tcms.enums.ExecutionStatus;
import com.tcms.repositories.UserRepository;
import com.tcms.security.ProjectAccess;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ExecutionPolicy {
    private final UserRepository users;
    private final ProjectAccess access;
    public ExecutionPolicy(UserRepository users, ProjectAccess access) { this.users=users; this.access=access; }
    public boolean manager() { return access.manager(); }
    public void requireManager() { if (!manager()) throw new AccessDeniedException("Manager access required"); }
    public Users currentUser() {
        var authentication=SecurityContextHolder.getContext().getAuthentication();
        Users user=authentication==null ? null : users.findByUserName(authentication.getName());
        if (user==null || !Boolean.TRUE.equals(user.getIsActive())) throw new AccessDeniedException("Active user required");
        return user;
    }
    public String currentName() {
        Users user=currentUser();
        return user.getFirstName()+" "+user.getLastName();
    }
    public boolean canRecord(TestCaseExecutions link) {
        if (link.getTestExecutions().getExecutionStatus()!=ExecutionStatus.IN_PROGRESS) return false;
        if (!Boolean.TRUE.equals(link.getTestExecutions().getReleasePlan())) return true;
        if (manager()) return true;
        Users user=currentUser();
        return access.project(link.getTestExecutions().getProjects().getId()) && link.getAssignee()!=null
                && link.getAssignee().getId().equals(user.getId());
    }
    public void requireRecord(TestCaseExecutions link) {
        requireOpen(link.getTestExecutions());
        if (link.getTestExecutions().getExecutionStatus()!=ExecutionStatus.IN_PROGRESS)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Start the plan before recording results");
        if (!canRecord(link)) throw new AccessDeniedException("This case is assigned to another tester");
    }
    public void requireOpen(TestExecutions plan) {
        if (plan.getExecutionStatus()==ExecutionStatus.CLOSED)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Reopen this plan before making changes");
    }
}
