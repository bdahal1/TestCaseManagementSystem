package com.tcms.services;

import com.tcms.models.TestSteps;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class TestStepService {

    public Map<String, Object> getTestStepsListResponse(Page<TestSteps> testStepsList) {
        Map<String, Object> response = new HashMap<>();
        response.put("testSteps", testStepsList.getContent());
        response.put("currentPage", testStepsList.getNumber());
        response.put("totalItems", testStepsList.getTotalElements());
        response.put("totalPages", testStepsList.getTotalPages());
        return response;
    }
}
