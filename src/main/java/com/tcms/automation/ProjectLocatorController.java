package com.tcms.automation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/automation/locators")
public class ProjectLocatorController {
    private final ProjectLocatorService locatorService;

    public ProjectLocatorController(ProjectLocatorService locatorService) {
        this.locatorService = locatorService;
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("@projectAccess.project(#p0)")
    public ResponseEntity<List<ProjectLocator>> getProjectLocators(@PathVariable int projectId) {
        return ResponseEntity.ok(locatorService.getProjectLocators(projectId));
    }

    @PostMapping("/project/{projectId}")
    @PreAuthorize("@projectAccess.project(#p0)")
    public ResponseEntity<ProjectLocator> createLocator(@PathVariable int projectId, @RequestBody ProjectLocator request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(locatorService.createLocator(projectId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectLocator> updateLocator(@PathVariable int id, @RequestBody ProjectLocator request) {
        return ResponseEntity.ok(locatorService.updateLocator(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLocator(@PathVariable int id) {
        locatorService.deleteLocator(id);
        return ResponseEntity.noContent().build();
    }
}
