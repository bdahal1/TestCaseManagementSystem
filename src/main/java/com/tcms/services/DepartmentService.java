package com.tcms.services;

import com.tcms.models.Department;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DepartmentService {

    public Map<String, Object> getDepartmentListResponse(Page<Department> departmentList) {
        Map<String, Object> response = new HashMap<>();
        response.put("departments", departmentList.getContent());
        response.put("currentPage", departmentList.getNumber());
        response.put("totalItems", departmentList.getTotalElements());
        response.put("totalPages", departmentList.getTotalPages());
        return response;
    }
}
