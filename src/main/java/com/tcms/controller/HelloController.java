package com.tcms.controller;

import com.tcms.models.Projects;
import com.tcms.models.Roles;
import com.tcms.models.TestCase;
import com.tcms.repositories.ProjectRepository;
import com.tcms.repositories.RoleRepository;
import com.tcms.repositories.TestCaseRepository;
import com.tcms.services.UserService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin
public class HelloController {
    final TestCaseRepository testCaseRepository;
    final RoleRepository roleRepository;
    final ProjectRepository projectRepository;
    final UserService userService;

    public HelloController(TestCaseRepository testCaseRepository, RoleRepository roleRepository, ProjectRepository projectRepository, UserService userService) {
        this.testCaseRepository = testCaseRepository;
        this.roleRepository = roleRepository;
        this.projectRepository = projectRepository;
        this.userService = userService;
    }

    @GetMapping("/testCases")
    public List<TestCase> getTestCases() {
        return testCaseRepository.findAll();
    }

    @GetMapping("/roles")
    public List<Roles> getRoles() {
        return roleRepository.findAll();
    }

    @GetMapping("/projects")
    public List<Projects> getProjects() {
        return projectRepository.findAll();
    }

}