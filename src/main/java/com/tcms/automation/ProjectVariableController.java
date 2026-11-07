package com.tcms.automation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/automation/variables")
public class ProjectVariableController {
    private final ProjectVariableService variableService;

    public ProjectVariableController(ProjectVariableService variableService) {
        this.variableService = variableService;
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("@projectAccess.project(#p0)")
    public ResponseEntity<List<ProjectVariableService.VariableView>> getProjectVariables(@PathVariable int projectId) {
        return ResponseEntity.ok(variableService.getProjectVariables(projectId));
    }

    @PostMapping("/project/{projectId}")
    @PreAuthorize("@projectAccess.project(#p0)")
    public ResponseEntity<ProjectVariableService.VariableView> saveVariable(@PathVariable int projectId, @RequestBody ProjectVariable request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(variableService.saveVariable(projectId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVariable(@PathVariable int id) {
        variableService.deleteVariable(id);
        return ResponseEntity.noContent().build();
    }
}
