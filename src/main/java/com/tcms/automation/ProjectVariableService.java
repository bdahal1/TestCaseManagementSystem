package com.tcms.automation;

import com.tcms.models.Projects;
import com.tcms.repositories.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class ProjectVariableService {
    private final ProjectVariableRepository repository;
    private final ProjectRepository projectRepository;
    private final com.tcms.security.ProjectAccess access;

    public ProjectVariableService(ProjectVariableRepository repository, ProjectRepository projectRepository, com.tcms.security.ProjectAccess access) {
        this.repository = repository;
        this.projectRepository = projectRepository;
        this.access = access;
    }

    public record VariableView(Integer id, String variableKey, String variableValue, boolean isSecret, String description) {}

    private void requireAccess(int projectId) {
        if (!access.project(projectId)) throw new org.springframework.security.access.AccessDeniedException("Project access required");
    }

    private VariableView view(ProjectVariable variable) {
        boolean secret=Boolean.TRUE.equals(variable.getIsSecret());
        return new VariableView(variable.getId(),variable.getVariableKey(),secret && variable.getVariableValue()!=null && !variable.getVariableValue().isBlank()
                ? "[CONFIGURED]" : variable.getVariableValue(),secret,variable.getDescription());
    }

    @Transactional(readOnly=true)
    public List<VariableView> getProjectVariables(int projectId) {
        requireAccess(projectId);
        return repository.findByProjectIdOrderByVariableKeyAsc(projectId).stream().map(this::view).toList();
    }

    public VariableView saveVariable(int projectId, ProjectVariable request) {
        requireAccess(projectId);
        Projects project = projectRepository.findById(projectId);
        if (project == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");

        String key = request.getVariableKey() != null ? request.getVariableKey().trim() : "";
        if (key.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Variable key is required");

        ProjectVariable variable = repository.findByProjectIdAndVariableKey(projectId, key)
                .orElse(new ProjectVariable());
        if (Boolean.TRUE.equals(variable.getIsSecret()) && !Boolean.TRUE.equals(request.getIsSecret())
                && (request.getVariableValue()==null || request.getVariableValue().equals("[CONFIGURED]")))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Provide a replacement value when changing a secret to a plain variable");
        variable.setProject(project);
        variable.setVariableKey(key);
        if (request.getVariableValue() != null && !request.getVariableValue().equals("[CONFIGURED]")) {
            variable.setVariableValue(request.getVariableValue());
        }
        variable.setIsSecret(Boolean.TRUE.equals(request.getIsSecret()));
        variable.setDescription(request.getDescription());
        return view(repository.save(variable));
    }

    public void deleteVariable(int id) {
        ProjectVariable variable = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Variable not found"));
        requireAccess(variable.getProject().getId());
        repository.delete(variable);
    }
}
