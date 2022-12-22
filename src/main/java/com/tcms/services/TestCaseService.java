package com.tcms.services;

import com.tcms.models.TestCase;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class TestCaseService {

    public Map<String, Object> getTestCaseListResponse(Page<TestCase> testCaseList) {
        Map<String, Object> response = new HashMap<>();
        response.put("testCase", testCaseList.getContent());
        response.put("currentPage", testCaseList.getNumber());
        response.put("totalItems", testCaseList.getTotalElements());
        response.put("totalPages", testCaseList.getTotalPages());
        return response;
    }
}
