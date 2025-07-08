package com.tcms.controller;

import com.tcms.dto.TestFolderDTO;
import com.tcms.dto.TestFolderResponseDTO;
import com.tcms.helper.pojo.CustomResponseMessage;
import com.tcms.models.Projects;
import com.tcms.models.TestCase;
import com.tcms.models.TestFolders;
import com.tcms.repositories.ProjectRepository;
import com.tcms.repositories.TestCaseRepository;
import com.tcms.repositories.TestFolderRepository;
import com.tcms.services.TestFolderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController()
@CrossOrigin()
@RequestMapping("/testFolders")
public class TestFoldersController {
    private final String defaultSize = "1000";
    private final TestFolderService testFolderService;
    private final TestFolderRepository testFolderRepository;
    private final ProjectRepository projectRepository;
    private final TestCaseRepository testCaseRepository;

    public TestFoldersController(TestFolderService testFolderService, TestFolderRepository testFolderRepository, ProjectRepository projectRepository, TestCaseRepository testCaseRepository) {
        this.testFolderService = testFolderService;
        this.testFolderRepository = testFolderRepository;
        this.projectRepository = projectRepository;
        this.testCaseRepository = testCaseRepository;
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<Object> getTestFolders(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = defaultSize) int size, @PathVariable String projectId) {
        Pageable paging = PageRequest.of(page, size);
        Page<TestFolders> testFoldersList = testFolderRepository.findByProjectsId(Integer.parseInt(projectId), paging);
        if (testFoldersList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK).body("Record not found.\n");
        }
        return ResponseEntity.status(HttpStatus.OK).body(testFolderService.getTestFolderListResponse(testFoldersList));
    }

    @GetMapping(path = "/name/{folderName}")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> getTestFoldersByName(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = defaultSize) int size, @PathVariable String folderName) {
        Pageable paging = PageRequest.of(page, size);
        Page<TestFolders> testFoldersList = testFolderRepository.findByFolderNameContaining(folderName, paging);
        if (testFoldersList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record not found.\n");
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(testFolderService.getTestFolderListResponse(testFoldersList));
        }
    }

    @GetMapping(path = "/id/{id}")
    public ResponseEntity<Object> getTestFolderById(@PathVariable int id) {
        TestFolders testFolders = testFolderRepository.findById(id);
        if (testFolders == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record not found.\n");
        } else {
            TestFolderResponseDTO testFolderResponseDTO = new TestFolderResponseDTO();
            testFolderResponseDTO.setId(testFolders.getId());
            testFolderResponseDTO.setFolderName(testFolders.getFolderName());
            testFolderResponseDTO.setTestCases(testFolders.getTestCaseSet());
            return ResponseEntity.status(HttpStatus.OK).body(testFolderResponseDTO);
        }
    }

    @PostMapping("")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> saveTestFolder(@RequestBody TestFolderDTO testFolderDTO) {
        if (testFolderDTO == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Test Folder Info Not Found inside body.\n");
        }
        Projects projects = projectRepository.findById(testFolderDTO.getProjectId());
        if (projects == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Project not found.\n");
        }
        try {
            TestFolders testFolders = new TestFolders();
            testFolders.setFolderName(testFolderDTO.getFolderName());
            testFolders.setProjects(projects);
            testFolderService.saveTestFolder(testFolders);
            return ResponseEntity.status(HttpStatus.OK).body(testFolders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CustomResponseMessage(new Date(), "Error", e.getCause().getCause().getLocalizedMessage()));
        }
    }

    @PostMapping("/{folderId}/addCases")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> saveTestCasesToFolder(@RequestBody Map<String, List<Integer>> body, @PathVariable String folderId) {
        List<Integer> testCaseIds = body.get("testCaseIds");
        if (testCaseIds == null && folderId == null) {
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
            TestFolders testFolders = testFolderRepository.findById(Integer.parseInt(folderId));
            testFolders.getTestCaseSet().addAll(testCaseSet);
            testFolderService.saveTestFolder(testFolders);
            return ResponseEntity.status(HttpStatus.OK).body(testFolders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CustomResponseMessage(new Date(), "Error", e.getCause().getCause().getLocalizedMessage()));
        }
    }

    @PostMapping("/{folderId}/removeCases")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> removeTestCasesFromFolder(@RequestBody Map<String, List<Integer>> body, @PathVariable String folderId) {
        List<Integer> testCaseIds = body.get("testCaseIds");
        if (testCaseIds == null && folderId == null) {
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
            TestFolders testFolders = testFolderRepository.findById(Integer.parseInt(folderId));
            testFolders.getTestCaseSet().removeAll(testCaseSet);
            testFolderService.saveTestFolder(testFolders);
            return ResponseEntity.status(HttpStatus.OK).body(testFolders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CustomResponseMessage(new Date(), "Error", e.getCause().getCause().getLocalizedMessage()));
        }
    }

    @PutMapping("/{folderId}")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> editTestFolder(@RequestBody TestFolderDTO testFolderDTO, @PathVariable int folderId) {
        if (testFolderDTO == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Test folder Info Not Found inside body.\n");
        }
        try {
            TestFolders testFolders = testFolderRepository.findById(folderId);
            if (testFolders == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Test Folder Not Found in database.\n");
            }
            testFolders.setFolderName(testFolderDTO.getFolderName() == null ? testFolders.getFolderName() : testFolderDTO.getFolderName());
            testFolderService.saveTestFolder(testFolders);
            return ResponseEntity.status(HttpStatus.OK).body(testFolders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CustomResponseMessage(new Date(), "Error", e.getCause().getCause().getLocalizedMessage()));
        }
    }

    @DeleteMapping("/{folderId}")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> deleteTestFolder(@PathVariable String folderId) {
        if (folderId == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Folder Id not found for delete operation.\n");
        }
        TestFolders testFolders = testFolderRepository.findById(Integer.parseInt(folderId));
        if (testFolders == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new CustomResponseMessage(new Date(), "Error", "Provided folder id not found for Delete operation!"));
        }
        testFolderRepository.deleteById(Integer.parseInt(folderId));
        return ResponseEntity.status(HttpStatus.OK).body(new CustomResponseMessage(new Date(), "Success", "Test Folder Deleted Successfully!"));
    }
}
