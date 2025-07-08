package com.tcms.controller;

import com.tcms.dto.TestCaseInfoDTO;
import com.tcms.helper.pojo.CustomResponseMessage;
import com.tcms.models.TestCase;
import com.tcms.models.TestFolders;
import com.tcms.repositories.ProjectRepository;
import com.tcms.repositories.TestCaseRepository;
import com.tcms.repositories.TestFolderRepository;
import com.tcms.repositories.UserRepository;
import com.tcms.services.TestCaseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Date;
import java.util.List;

@RestController()
@CrossOrigin()
@RequestMapping("/testCase")
public class TestCaseController {
    private final String defaultSize = "1000";
    private final TestCaseRepository testCaseRepository;
    private final TestCaseService testCaseService;

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TestFolderRepository testFolderRepository;


    public TestCaseController(TestCaseRepository testCaseRepository, TestCaseService testCaseService, UserRepository userRepository, ProjectRepository projectRepository, TestFolderRepository testFolderRepository) {
        this.testCaseRepository = testCaseRepository;
        this.testCaseService = testCaseService;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.testFolderRepository = testFolderRepository;
    }

    @GetMapping("")
    public ResponseEntity<Object> getTestCase(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = defaultSize) int size, @RequestParam String projectId) {
        Pageable paging = PageRequest.of(page, size);
        Page<TestCase> testCaseList = testCaseRepository.findByProjectsIn(Collections.singleton(projectRepository.findById(Integer.parseInt(projectId))), paging);
        if (testCaseList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK).body("Record not found.\n");
        }
        return ResponseEntity.status(HttpStatus.OK).body(testCaseService.getTestCaseListResponse(testCaseList));
    }

    @GetMapping("/unassigned")
    public ResponseEntity<Object> getTestCaseNotInAnyFolder(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = defaultSize) int size, @RequestParam String projectId) {
        Pageable pageable = PageRequest.of(page, size);
        List<TestCase> testCaseList = testCaseRepository.findByProjectsIn(Collections.singleton(projectRepository.findById(Integer.parseInt(projectId))));
        if (testCaseList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK).body("Record not found.\n");
        }
        List<TestFolders> testFoldersList = testFolderRepository.findByProjectsId(Integer.parseInt(projectId));
        List<TestCase> testCaseListInFolders = testFoldersList.stream()
                .flatMap(folder -> folder.getTestCaseSet().stream())
                .distinct()
                .toList();
        List<TestCase> testCasesNotInFolders = testCaseList.stream()
                .filter(tc -> !testCaseListInFolders.contains(tc))
                .toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), testCasesNotInFolders.size());

        List<TestCase> content = (start <= end) ? testCasesNotInFolders.subList(start, end) : Collections.emptyList();

        return ResponseEntity.status(HttpStatus.OK).body(testCaseService.getTestCaseListResponse(new PageImpl<>(content, pageable, testCasesNotInFolders.size())));
    }

    @GetMapping(path = "/name/{testCaseName}")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> getTestCaseByTestCaseName(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = defaultSize) int size, @PathVariable String testCaseName) {
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
