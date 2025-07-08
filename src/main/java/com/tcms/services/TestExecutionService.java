package com.tcms.services;

import com.tcms.models.TestExecutions;
import com.tcms.repositories.TestExecutionRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class TestExecutionService {
    final public TestExecutionRepository testExecutionRepository;

    public TestExecutionService(TestExecutionRepository testExecutionRepository) {
        this.testExecutionRepository = testExecutionRepository;
    }

    public Map<String, Object> getTestExecutionListResponse(Page<TestExecutions> testExecutions) {
        Map<String, Object> response = new HashMap<>();
        response.put("testExecutions", testExecutions.getContent());
        response.put("currentPage", testExecutions.getNumber());
        response.put("totalItems", testExecutions.getTotalElements());
        response.put("totalPages", testExecutions.getTotalPages());
        return response;
    }

    public TestExecutions saveTestExecution(TestExecutions testExecutions) {
        return testExecutionRepository.save(testExecutions);
    }
}
