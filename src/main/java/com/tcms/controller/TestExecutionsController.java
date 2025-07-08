package com.tcms.controller;

import com.tcms.dto.TestExecutionDTO;
import com.tcms.dto.TestExecutionResponseDTO;
import com.tcms.helper.pojo.CustomResponseMessage;
import com.tcms.models.Projects;
import com.tcms.models.TestCase;
import com.tcms.models.TestExecutions;
import com.tcms.repositories.ProjectRepository;
import com.tcms.repositories.TestCaseRepository;
import com.tcms.repositories.TestExecutionRepository;
import com.tcms.services.TestExecutionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController()
@CrossOrigin()
@RequestMapping("/testExecutions")
public class TestExecutionsController {
    private final String defaultSize = "1000";
    private final TestExecutionService testExecutionService;
    private final TestExecutionRepository testExecutionRepository;
    private final ProjectRepository projectRepository;
    private final TestCaseRepository testCaseRepository;

    public TestExecutionsController(TestExecutionService testExecutionService, TestExecutionRepository testExecutionRepository, ProjectRepository projectRepository, TestCaseRepository testCaseRepository) {
        this.testExecutionService = testExecutionService;
        this.testExecutionRepository = testExecutionRepository;
        this.projectRepository = projectRepository;
        this.testCaseRepository = testCaseRepository;
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<Object> getTestExecutions(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = defaultSize) int size, @PathVariable String projectId) {
        Pageable paging = PageRequest.of(page, size);
        Page<TestExecutions> testExecutionsPage = testExecutionRepository.findByProjectsId(Integer.parseInt(projectId), paging);
        if (testExecutionsPage.isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK).body("Record not found.\n");
        }
        return ResponseEntity.status(HttpStatus.OK).body(testExecutionService.getTestExecutionListResponse(testExecutionsPage));
    }

    @GetMapping(path = "/name/{folderName}")
    @SuppressWarnings("Duplicates")
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
    public ResponseEntity<Object> getTestExecutionById(@PathVariable int id) {
        TestExecutions testExecutions = testExecutionRepository.findById(id);
        if (testExecutions == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record not found.\n");
        } else {
            TestExecutionResponseDTO testExecutionResponseDTO = new TestExecutionResponseDTO();
            testExecutionResponseDTO.setId(testExecutions.getId());
            testExecutionResponseDTO.setExecutionName(testExecutions.getExecutionName());
            testExecutionResponseDTO.setTestCases(testExecutions.getTestCaseSet());
            return ResponseEntity.status(HttpStatus.OK).body(testExecutionResponseDTO);
        }
    }

    @PostMapping("")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> saveTestExecution(@RequestBody TestExecutionDTO testExecutionDTO) {
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
            testExecutions.setProjects(projects);
            testExecutionService.saveTestExecution(testExecutions);
            return ResponseEntity.status(HttpStatus.OK).body(testExecutions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CustomResponseMessage(new Date(), "Error", e.getCause().getCause().getLocalizedMessage()));
        }
    }

    @PostMapping("/{executionId}/addCases")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> saveTestCasesToExecution(@RequestBody Map<String, List<Integer>> body, @PathVariable String executionId) {
        List<Integer> testCaseIds = body.get("testCaseIds");
        if (testCaseIds == null && executionId == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Test case id list Not Found inside body.\n");
        }
        try {
            Set<TestCase> testCaseSet = new HashSet<>();
            for (Integer i : testCaseIds) {
                TestCase testCase = testCaseRepository.findById(i);
                if (testCase == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Test case not found.\n");
                }
                testCaseSet.add(testCase);
            }
            TestExecutions testExecutions = testExecutionRepository.findById(Integer.parseInt(executionId));
            testExecutions.getTestCaseSet().addAll(testCaseSet);
            testExecutionService.saveTestExecution(testExecutions);
            return ResponseEntity.status(HttpStatus.OK).body(testExecutions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CustomResponseMessage(new Date(), "Error", e.getCause().getCause().getLocalizedMessage()));
        }
    }

    @PostMapping("/{executionId}/removeCases")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> removeTestCasesFromExecution(@RequestBody Map<String, List<Integer>> body, @PathVariable String executionId) {
        List<Integer> testCaseIds = body.get("testCaseIds");
        if (testCaseIds == null && executionId == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Test case id list Not Found inside body.\n");
        }
        try {
            Set<TestCase> testCaseSet = new HashSet<>();
            for (Integer i : testCaseIds) {
                TestCase testCase = testCaseRepository.findById(i);
                if (testCase == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Test case not found.\n");
                }
                testCaseSet.add(testCase);
            }
            TestExecutions testExecutions = testExecutionRepository.findById(Integer.parseInt(executionId));
            testExecutions.getTestCaseSet().removeAll(testCaseSet);
            testExecutionService.saveTestExecution(testExecutions);
            return ResponseEntity.status(HttpStatus.OK).body(testExecutions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CustomResponseMessage(new Date(), "Error", e.getCause().getCause().getLocalizedMessage()));
        }
    }

    @PutMapping("/{executionId}")
    @SuppressWarnings("Duplicates")
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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CustomResponseMessage(new Date(), "Error", e.getCause().getCause().getLocalizedMessage()));
        }
    }

    @DeleteMapping("/{executionId}")
    @SuppressWarnings("Duplicates")
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
