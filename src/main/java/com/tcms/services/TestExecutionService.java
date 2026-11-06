package com.tcms.services;

import com.tcms.dto.TestResultRequestDTO;
import com.tcms.models.TestCaseExecutions;
import com.tcms.models.TestExecutions;
import com.tcms.repositories.TestCaseExecutionRepository;
import com.tcms.repositories.TestExecutionRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class TestExecutionService {
    private final com.tcms.repositories.TestCaseRepository testCaseRepository;
    private final ExecutionPolicy policy;
    private final TestCaseSnapshots snapshots;

    final public TestExecutionRepository testExecutionRepository;
    final public TestCaseExecutionRepository testCaseExecutionRepository;

    public TestExecutionService(TestExecutionRepository testExecutionRepository, TestCaseExecutionRepository testCaseExecutionRepository, com.tcms.repositories.TestCaseRepository testCaseRepository,
                                ExecutionPolicy policy, TestCaseSnapshots snapshots) {
        this.policy=policy;
        this.snapshots=snapshots;
        this.testCaseRepository = testCaseRepository;
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
        if (Boolean.TRUE.equals(testExecutions.getReleasePlan())) {
            policy.requireManager(); policy.requireOpen(testExecutions);
        }
        return testExecutionRepository.save(testExecutions);
    }

    @org.springframework.transaction.annotation.Transactional
    public TestCaseExecutions addResultToTestCase(TestResultRequestDTO request) {
        lockedExecution(request.getExecutionId());
        TestCaseExecutions tce = testCaseExecutionRepository.findByTestExecutionsIdAndTestCaseId(
                request.getExecutionId(), request.getTestCaseId()
        ).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Test case execution not found"));
        policy.requireRecord(tce);
        if (Boolean.TRUE.equals(tce.getTestExecutions().getReleasePlan()) || (tce.getResultRevision()!=null && tce.getResultRevision()>0))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"Record this result in the test plan runner");
        snapshots.capture(tce);
        if (request.getResultStatus()==null) throw new IllegalArgumentException("A result is required");
        tce.setResultComment(request.getResultComment());
        tce.setResultStatus(request.getResultStatus());
        return testCaseExecutionRepository.save(tce);
    }

    public com.tcms.dto.plans.PlanResponses.Snapshot getCaseSnapshot(TestCaseExecutions link) {
        return snapshots.read(link);
    }

    private TestExecutions lockedExecution(int id) {
        return testExecutionRepository.findForUpdate(id).orElseThrow(() ->
                new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Execution not found"));
    }

    @org.springframework.transaction.annotation.Transactional
    public TestExecutions addCases(int executionId, java.util.List<Integer> caseIds) {
        TestExecutions execution = lockedExecution(executionId);
        policy.requireOpen(execution);
        if (Boolean.TRUE.equals(execution.getReleasePlan())) policy.requireManager();
        if (caseIds == null) throw new IllegalArgumentException("Test case IDs are required");
        java.util.List<com.tcms.models.TestCase> cases = new java.util.ArrayList<>();
        // Validate the complete batch before changing any association.
        for (Integer id : new java.util.LinkedHashSet<>(caseIds)) {
            if (id == null) throw new IllegalArgumentException("Test case ID is required");
            com.tcms.models.TestCase tc = testCaseRepository.findById(id.intValue());
            if (tc == null) throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Test case not found");
            if (tc.getProjects() == null || !java.util.Objects.equals(tc.getProjects().getId(), execution.getProjects().getId()))
                throw new IllegalArgumentException("Test cases must belong to the execution project");
            cases.add(tc);
        }
        for (com.tcms.models.TestCase tc : cases) {
            if (execution.getTestCaseExecutions().stream().noneMatch(link -> link.getTestCase().getId().equals(tc.getId()))) {
                TestCaseExecutions link = new TestCaseExecutions();
                link.setTestExecutions(execution);
                link.setTestCase(tc);
                snapshots.capture(link);
                execution.getTestCaseExecutions().add(link);
            }
        }
        return testExecutionRepository.save(execution);
    }

    @org.springframework.transaction.annotation.Transactional
    public TestExecutions removeCases(int executionId, java.util.List<Integer> caseIds) {
        if (caseIds == null) throw new IllegalArgumentException("Test case IDs are required");
        TestExecutions execution = lockedExecution(executionId);
        policy.requireOpen(execution);
        if (Boolean.TRUE.equals(execution.getReleasePlan())) policy.requireManager();
        if (Boolean.TRUE.equals(execution.getReleasePlan()) && execution.getTestCaseExecutions().stream()
                .filter(link -> caseIds.contains(link.getTestCase().getId()))
                .anyMatch(link -> (link.getResultStatus()!=null && link.getResultStatus()!=com.tcms.enums.TestResult.NOT_RUN)
                        || !link.getHistory().isEmpty() || !link.getEvidence().isEmpty()))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
                    "Cases with results or evidence must remain in the plan");
        execution.getTestCaseExecutions().removeIf(link -> caseIds.contains(link.getTestCase().getId()));
        return testExecutionRepository.save(execution);
    }
}
