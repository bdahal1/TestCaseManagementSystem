package com.tcms.services;

import com.tcms.dto.TestResultRequestDTO;
import com.tcms.models.TestCaseExecutions;
import com.tcms.models.TestExecutionResults;
import com.tcms.models.TestExecutions;
import com.tcms.repositories.TestCaseExecutionRepository;
import com.tcms.repositories.TestExecutionRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class TestExecutionService {
    final public TestExecutionRepository testExecutionRepository;
    final public TestCaseExecutionRepository testCaseExecutionRepository;

    public TestExecutionService(TestExecutionRepository testExecutionRepository, TestCaseExecutionRepository testCaseExecutionRepository) {
        this.testExecutionRepository = testExecutionRepository;
        this.testCaseExecutionRepository = testCaseExecutionRepository;
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

    public TestCaseExecutions addResultToTestCase(TestResultRequestDTO request) {
        TestCaseExecutions tce = testCaseExecutionRepository.findByTestExecutionsIdAndTestCaseId(
                request.getExecutionId(), request.getTestCaseId()
        ).orElseThrow(() -> new RuntimeException("Test case execution not found"));

        TestExecutionResults result = new TestExecutionResults();
        result.setResultStatus(request.getResultStatus());
        result.setResultComment(request.getResultComment());

        tce.setResult(result);
        return testCaseExecutionRepository.save(tce); // Cascade will save the result too
    }
}
