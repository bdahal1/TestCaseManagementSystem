package com.tcms.automation;

import com.tcms.models.Projects;
import com.tcms.repositories.ProjectRepository;
import com.tcms.services.ExecutionPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class ProjectLocatorService {
    private final ProjectLocatorRepository repository;
    private final ProjectRepository projectRepository;
    private final ExecutionPolicy policy;
    private final com.tcms.security.ProjectAccess access;

    public ProjectLocatorService(ProjectLocatorRepository repository, ProjectRepository projectRepository, ExecutionPolicy policy, com.tcms.security.ProjectAccess access) {
        this.repository = repository;
        this.projectRepository = projectRepository;
        this.policy = policy;
        this.access = access;
    }

    private void requireAccess(int projectId) {
        if (!access.project(projectId)) throw new org.springframework.security.access.AccessDeniedException("Project access required");
    }

    public List<ProjectLocator> getProjectLocators(int projectId) {
        requireAccess(projectId);
        return repository.findByProjectIdOrderByLocatorNameAsc(projectId);
    }

    public ProjectLocator createLocator(int projectId, ProjectLocator request) {
        requireAccess(projectId);
        Projects project = projectRepository.findById(projectId);
        if (project == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");

        String name = request.getLocatorName() != null ? request.getLocatorName().trim() : "";
        if (name.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Locator name is required");
        if (request.getLocatorType() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Locator type is required");
        if (request.getLocatorValue() == null || request.getLocatorValue().trim().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Locator value is required");

        if (repository.existsByProjectIdAndLocatorName(projectId, name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Locator '" + name + "' already exists in this project");
        }

        ProjectLocator locator = new ProjectLocator();
        locator.setProject(project);
        locator.setLocatorName(name);
        locator.setLocatorType(request.getLocatorType());
        locator.setLocatorValue(request.getLocatorValue().trim());
        locator.setDescription(request.getDescription());
        locator.setCreatedBy(policy.currentName());
        locator.setCreatedDate(Timestamp.from(Instant.now()));
        locator.setModifiedBy(locator.getCreatedBy());
        locator.setModifiedDate(locator.getCreatedDate());
        return repository.save(locator);
    }

    public ProjectLocator updateLocator(int id, ProjectLocator request) {
        ProjectLocator locator = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Locator not found"));
        requireAccess(locator.getProject().getId());

        if (request.getLocatorName() != null && !request.getLocatorName().trim().isEmpty()) {
            String name = request.getLocatorName().trim();
            if (!name.equals(locator.getLocatorName()) && repository.existsByProjectIdAndLocatorName(locator.getProject().getId(), name)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Locator '" + name + "' already exists in this project");
            }
            locator.setLocatorName(name);
        }
        if (request.getLocatorType() != null) {
            locator.setLocatorType(request.getLocatorType());
        }
        if (request.getLocatorValue() != null && !request.getLocatorValue().trim().isEmpty()) {
            locator.setLocatorValue(request.getLocatorValue().trim());
        }
        if (request.getDescription() != null) {
            locator.setDescription(request.getDescription());
        }
        locator.setModifiedBy(policy.currentName());
        locator.setModifiedDate(Timestamp.from(Instant.now()));
        return repository.save(locator);
    }

    public void deleteLocator(int id) {
        ProjectLocator locator = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Locator not found"));
        requireAccess(locator.getProject().getId());
        repository.delete(locator);
    }
}
