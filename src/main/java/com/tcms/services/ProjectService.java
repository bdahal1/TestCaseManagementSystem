package com.tcms.services;

import com.tcms.models.Projects;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ProjectService {

    public Map<String, Object> getProjectListResponse(Page<Projects> projectList) {
        Map<String, Object> response = new HashMap<>();
        response.put("projects", projectList.getContent());
        response.put("currentPage", projectList.getNumber());
        response.put("totalItems", projectList.getTotalElements());
        response.put("totalPages", projectList.getTotalPages());
        return response;
    }
}
