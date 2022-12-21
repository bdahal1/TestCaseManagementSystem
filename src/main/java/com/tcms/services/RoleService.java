package com.tcms.services;

import com.tcms.models.Roles;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class RoleService {
    public Map<String, Object> getRoleListResponse(Page<Roles> rolesList) {
        Map<String, Object> response = new HashMap<>();
        response.put("roles", rolesList.getContent());
        response.put("currentPage", rolesList.getNumber());
        response.put("totalItems", rolesList.getTotalElements());
        response.put("totalPages", rolesList.getTotalPages());
        return response;
    }
}
