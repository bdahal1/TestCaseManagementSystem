package com.tcms.controller;

import com.tcms.dto.TestCaseInfoDTO;
import com.tcms.helper.pojo.CustomResponseMessage;
import com.tcms.models.Projects;
import com.tcms.models.TestCase;
import com.tcms.models.Users;
import com.tcms.repositories.ProjectRepository;
import com.tcms.repositories.TestCaseRepository;
import com.tcms.repositories.UserRepository;
import com.tcms.services.TestCaseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Date;

@RestController()
@CrossOrigin()
@RequestMapping("/testCase")
public class TestCaseController {
    private final TestCaseRepository testCaseRepository;
    private final TestCaseService testCaseService;

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;


    public TestCaseController(TestCaseRepository testCaseRepository, TestCaseService testCaseService, UserRepository userRepository, ProjectRepository projectRepository) {
        this.testCaseRepository = testCaseRepository;
        this.testCaseService = testCaseService;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
    }

    @GetMapping("")
    public ResponseEntity<Object> getTestCase(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "1000") int size, @RequestParam String projectId) {
        Pageable paging = PageRequest.of(page, size);
        Page<TestCase> testCaseList = testCaseRepository.findByProjectsIn(Collections.singleton(projectRepository.findById(Integer.parseInt(projectId))),paging);
        if (testCaseList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK).body("Record not found.\n");
        }
        return ResponseEntity.status(HttpStatus.OK).body(testCaseService.getTestCaseListResponse(testCaseList));
    }

    @GetMapping(path = "/name/{testCaseName}")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> getTestCaseByTestCaseName(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "1000") int size, @PathVariable String testCaseName) {
        Pageable paging = PageRequest.of(page, size);
        Page<TestCase> testCasesList = testCaseRepository.findByTestNameContaining(testCaseName, paging);
        if (testCasesList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record not found.\n");
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(testCaseService.getTestCaseListResponse(testCasesList));
        }
    }

    @GetMapping(path = "/id/{id}")
    public ResponseEntity<Object> getTestCaseById(@PathVariable int id) {
        TestCase testCase = testCaseRepository.findById(id);
        if (testCase == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record not found.\n");
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(testCase);
        }
    }

    @PostMapping("")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> saveTestCase(@RequestBody TestCaseInfoDTO testCaseInfoDTO) {
        if (testCaseInfoDTO == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Testcase Info Not Found inside body.\n");
        }
        try {
            if (testCaseInfoDTO.getTestName() == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Testcase Name Not Found inside body.\n");
            }
            return testCaseService.saveTestCase(testCaseInfoDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CustomResponseMessage(new Date(), "Error", e.getCause().getCause().getLocalizedMessage()));
        }
    }

    @PutMapping("/{testCaseId}")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> editTestCase(@RequestBody TestCaseInfoDTO testCaseInfoDTO, @PathVariable int testCaseId) {
        if (testCaseInfoDTO == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Testcase Info Not Found inside body.\n");
        }
        try {
            return testCaseService.editTestCase(testCaseInfoDTO, testCaseId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CustomResponseMessage(new Date(), "Error", e.getCause().getCause().getLocalizedMessage()));
        }
    }

    @DeleteMapping("/{testCaseId}")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> deleteTestCase(@PathVariable String testCaseId) {
        if (testCaseId == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("testCaseId not found for delete operation.\n");
        }
        TestCase testCase = testCaseRepository.findById(Integer.parseInt(testCaseId));
        if (testCase == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new CustomResponseMessage(new Date(), "Error", "Provided testcase not found for Delete operation!"));
        }
        testCaseRepository.deleteById(testCase.getId());
        return ResponseEntity.status(HttpStatus.OK).body(new CustomResponseMessage(new Date(), "Success", "Testcase Deleted Successfully!"));
    }
}
