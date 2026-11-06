package com.tcms.controller;

import org.springframework.security.access.prepost.PreAuthorize;

import com.tcms.dto.*;
import com.tcms.enums.ExecutionStatus;
import com.tcms.helper.pojo.CustomResponseMessage;
import com.tcms.models.*;
import com.tcms.repositories.ProjectRepository;
import com.tcms.repositories.TestExecutionRepository;
import com.tcms.services.TestExecutionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController()
@RequestMapping("/testExecutions")
public class TestExecutionsController {
    private final String defaultSize = "1000";
    private final TestExecutionService testExecutionService;
    private final TestExecutionRepository testExecutionRepository;
    private final ProjectRepository projectRepository;

    public TestExecutionsController(TestExecutionService testExecutionService, TestExecutionRepository testExecutionRepository, ProjectRepository projectRepository) {
        this.testExecutionService = testExecutionService;
        this.testExecutionRepository = testExecutionRepository;
        this.projectRepository = projectRepository;
    }

    @PostMapping("/results")
    @PreAuthorize("@projectAccess.result(#p0)")
    public ResponseEntity<Object> addResult(@jakarta.validation.Valid @RequestBody TestResultRequestDTO request) {
        testExecutionService.addResultToTestCase(request);
        return ResponseEntity.ok("Result added successfully.");
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("@projectAccess.project(#p2)")
    public ResponseEntity<Object> getTestExecutions(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = defaultSize) int size, @PathVariable String projectId) {
        Pageable paging = PageRequest.of(page, size);
        Page<TestExecutions> testExecutionsPage = testExecutionRepository.findByProjectsIdAndExecutionStatus(Integer.parseInt(projectId), ExecutionStatus.IN_PROGRESS, paging);
        if (testExecutionsPage.isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK).body("Record not found.\n");
        }
        return ResponseEntity.status(HttpStatus.OK).body(testExecutionService.getTestExecutionListResponse(testExecutionsPage));
    }

    @GetMapping(path = "/name/{executionName}")
    @SuppressWarnings("Duplicates")
    @PreAuthorize("@projectAccess.manager()")
    public ResponseEntity<Object> getTestExecutionsByName(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = defaultSize) int size, @PathVariable String executionName) {
        Pageable paging = PageRequest.of(page, size);
        Page<TestExecutions> testExecutionsPage = testExecutionRepository.findByExecutionNameContaining(executionName, paging);
        if (testExecutionsPage.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record not found.\n");
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(testExecutionService.getTestExecutionListResponse(testExecutionsPage));
        }
    }

    @GetMapping(path = "/id/{id}")
    @PreAuthorize("@projectAccess.execution(#p0)")
    public ResponseEntity<Object> getTestExecutionById(@PathVariable int id) {
        TestExecutions testExecutions = testExecutionRepository.findById(id);
        if (testExecutions == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record not found.\n");
        } else {
            TestExecutionResponseDTO testExecutionResponseDTO = new TestExecutionResponseDTO();
            testExecutionResponseDTO.setId(testExecutions.getId());
            testExecutionResponseDTO.setExecutionName(testExecutions.getExecutionName());
            Set<TestCaseWithResultDTO> testCaseDTOs = testExecutions.getTestCaseExecutions().stream()
                    .map(tce -> {
                        TestCase tc = tce.getTestCase();
                        var snapshot=testExecutionService.getCaseSnapshot(tce);
                        Set<TestStepInfoDTO> testStepInfoDTOSet = new HashSet<>();
                        for (var step:snapshot.steps()) {
                            TestStepInfoDTO dto=new TestStepInfoDTO();
                            dto.setTestStepData(step.data()); dto.setTestStepDesc(step.instruction()); dto.setStepOrder(step.order());
                            dto.setTestExpectedOutput(step.expected()); dto.setTestType(snapshot.type()); testStepInfoDTOSet.add(dto);
                        }
                        return new TestCaseWithResultDTO(tc.getId(),snapshot.name(),testStepInfoDTOSet,tce.getResultStatus(),snapshot.type(),tce.getResultComment());
                    })
                    .collect(Collectors.toSet());
            testExecutionResponseDTO.setTestCases(testCaseDTOs);
            return ResponseEntity.status(HttpStatus.OK).body(testExecutionResponseDTO);
        }
    }

    @PostMapping("")
    @SuppressWarnings("Duplicates")
    @PreAuthorize("@projectAccess.project(#p0.projectId)")
    public ResponseEntity<Object> saveTestExecution(@jakarta.validation.Valid @RequestBody TestExecutionDTO testExecutionDTO) {
        if (testExecutionDTO == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Test Folder Info Not Found inside body.\n");
        }
        Projects projects = projectRepository.findById(testExecutionDTO.getProjectId());
        if (projects == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Project not found.\n");
        }
        try {
            TestExecutions testExecutions = new TestExecutions();
            testExecutions.setExecutionName(testExecutionDTO.getExecutionName());
            testExecutions.setExecutionStatus(testExecutionDTO.getExecutionStatus() == null ? ExecutionStatus.IN_PROGRESS : testExecutionDTO.getExecutionStatus());
            testExecutions.setProjects(projects);
            return ResponseEntity.status(HttpStatus.OK).body(testExecutionService.saveTestExecution(testExecutions));
        } catch (Exception e) {
            throw new IllegalStateException("Operation failed", e);
        }
    }

    @PostMapping("/{executionId}/addCases")
    @SuppressWarnings("Duplicates")
    @PreAuthorize("@projectAccess.links(#p1, #p0, true)")
    public ResponseEntity<Object> saveTestCasesToExecution(@RequestBody Map<String, List<Integer>> body, @PathVariable String executionId) {
        return ResponseEntity.ok(testExecutionService.addCases(Integer.parseInt(executionId), body.get("testCaseIds")));
    }

    @PostMapping("/{executionId}/removeCases")
    @SuppressWarnings("Duplicates")
    @PreAuthorize("@projectAccess.links(#p1, #p0, true)")
    public ResponseEntity<Object> removeTestCasesFromExecution(
            @RequestBody Map<String, List<Integer>> body,
            @PathVariable String executionId) {

        return ResponseEntity.ok(testExecutionService.removeCases(Integer.parseInt(executionId), body.get("testCaseIds")));
    }

    @PutMapping("/{executionId}")
    @SuppressWarnings("Duplicates")
    @PreAuthorize("@projectAccess.manageExecution(#p1)")
    public ResponseEntity<Object> editTestExecution(@RequestBody TestExecutionDTO testExecutionDTO, @PathVariable int executionId) {
        if (testExecutionDTO == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Test Execution Info Not Found inside body.\n");
        }
        try {
            TestExecutions testExecutions = testExecutionRepository.findById(executionId);
            if (testExecutions == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Test Execution Not Found in database.\n");
            }
            testExecutions.setExecutionName(testExecutionDTO.getExecutionName() == null ? testExecutions.getExecutionName() : testExecutionDTO.getExecutionName());
            testExecutionService.saveTestExecution(testExecutions);
            return ResponseEntity.status(HttpStatus.OK).body(testExecutions);
        } catch (Exception e) {
            throw new IllegalStateException("Operation failed", e);
        }
    }

    @DeleteMapping("/{executionId}")
    @SuppressWarnings("Duplicates")
    @PreAuthorize("@projectAccess.manageExecution(#p0)")
    public ResponseEntity<Object> deleteTestExecution(@PathVariable String executionId) {
        if (executionId == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Execution Id not found for delete operation.\n");
        }
        TestExecutions testExecutions = testExecutionRepository.findById(Integer.parseInt(executionId));
        if (testExecutions == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new CustomResponseMessage(new Date(), "Error", "Provided Execution id not found for Delete operation!"));
        }
        testExecutionRepository.deleteById(Integer.parseInt(executionId));
        return ResponseEntity.status(HttpStatus.OK).body(new CustomResponseMessage(new Date(), "Success", "Test Execution Deleted Successfully!"));
    }
}
