package com.tcms.controller;

import com.tcms.helper.pojo.CustomResponseMessage;
import com.tcms.models.Projects;
import com.tcms.repositories.ProjectRepository;
import com.tcms.services.ProjectService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController()
@CrossOrigin()
@RequestMapping("/project")
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final ProjectService projectService;

    public ProjectController(ProjectRepository projectRepository, ProjectService projectService) {
        this.projectRepository = projectRepository;
        this.projectService = projectService;
    }

    @GetMapping("")
    public ResponseEntity<Object> getProjects(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Pageable paging = PageRequest.of(page, size);
        Page<Projects> projectList = projectRepository.findAll(paging);
        if (projectList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record not found.\n");
        }
        return ResponseEntity.status(HttpStatus.OK).body(projectService.getProjectListResponse(projectList));
    }

    @GetMapping(path = "/projName/{projectName}")
    public ResponseEntity<Object> getProjectsByProjectName(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, @PathVariable String projectName) {
        Pageable paging = PageRequest.of(page, size);
        Page<Projects> projectList = projectRepository.findByProjectNameContaining(projectName, paging);
        if (projectList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record not found.\n");
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(projectService.getProjectListResponse(projectList));
        }
    }

    @GetMapping(path = "/id/{id}")
    public ResponseEntity<Object> getProjectById(@PathVariable int id) {
        Projects projects = projectRepository.findById(id);
        if (projects == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record not found.\n");
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(projects);
        }
    }

    @PostMapping("")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> saveProjects(@RequestBody Projects projects) {
        if (projects == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Project Info Not Found inside body.\n");
        }
        try {
            projectRepository.save(projects);
            return ResponseEntity.status(HttpStatus.OK).body(projects);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CustomResponseMessage(new Date(), "Error", e.getCause().getCause().getLocalizedMessage()));
        }
    }

    @PutMapping("/{projId}")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> editProject(@RequestBody Projects projects, @PathVariable int projId) {
        if (projects == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Project Info Not Found inside body.\n");
        }
        try {
            Projects project = projectRepository.findById(projId);
            if (project == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Project Not Found in database.\n");
            }
            project.setProjectName(projects.getProjectName() == null ? projects.getProjectName() : project.getProjectName());
            projectRepository.save(project);
            return ResponseEntity.status(HttpStatus.OK).body(project);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CustomResponseMessage(new Date(), "Error", e.getCause().getCause().getLocalizedMessage()));
        }
    }

    @DeleteMapping("/{projId}")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> deleteProject(@PathVariable String projId) {
        if (projId == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("projId not found for delete operation.\n");
        }
        Projects projects = projectRepository.findById(Integer.parseInt(projId));
        if (projects == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new CustomResponseMessage(new Date(), "Error", "Provided Project not found for Delete operation!"));
        }
        projectRepository.deleteById(Integer.parseInt(projId));
        return ResponseEntity.status(HttpStatus.OK).body(new CustomResponseMessage(new Date(), "Success", "Project Deleted Successfully!"));
    }
}
