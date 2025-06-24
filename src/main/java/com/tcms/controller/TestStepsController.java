package com.tcms.controller;

import com.tcms.dto.TestStepInfoDTO;
import com.tcms.dto.TestStepOrderDTO;
import com.tcms.helper.pojo.CustomResponseMessage;
import com.tcms.helper.util.Util;
import com.tcms.models.TestCase;
import com.tcms.models.TestSteps;
import com.tcms.models.Users;
import com.tcms.repositories.TestCaseRepository;
import com.tcms.repositories.TestStepsRepository;
import com.tcms.repositories.UserRepository;
import com.tcms.services.TestStepService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController()
@CrossOrigin()
@RequestMapping("/testSteps")
public class TestStepsController {
    private final TestStepsRepository testStepsRepository;
    private final TestStepService testStepService;

    private final TestCaseRepository testCaseRepository;
    private final UserRepository userRepository;

    public TestStepsController(TestStepsRepository testStepsRepository, TestStepService testStepService, TestCaseRepository testCaseRepository, UserRepository userRepository) {
        this.testStepsRepository = testStepsRepository;
        this.testStepService = testStepService;
        this.testCaseRepository = testCaseRepository;
        this.userRepository = userRepository;
    }


    @GetMapping("")
    public ResponseEntity<Object> getTestStep(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Pageable paging = PageRequest.of(page, size);
        Page<TestSteps> testStepsList = testStepsRepository.findAll(paging);
        if (testStepsList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record not found.\n");
        }
        return ResponseEntity.status(HttpStatus.OK).body(testStepService.getTestStepsListResponse(testStepsList));
    }

    @GetMapping(path = "/id/{id}")
    public ResponseEntity<Object> getTestStepsById(@PathVariable int id) {
        TestSteps testSteps = testStepsRepository.findById(id);
        if (testSteps == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record not found.\n");
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(testSteps);
        }
    }

    @GetMapping(path = "/testCaseId/{id}")
    public ResponseEntity<Object> getTestStepsByTestCaseId(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, @PathVariable int id) {
        Pageable paging = PageRequest.of(page, size);
        TestCase testCase = testCaseRepository.findById(id);
        if (testCase == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record not found.\n");
        } else {
            Page<TestSteps> testStepsList = testStepsRepository.findAllByTestCase(testCase, paging);
            return ResponseEntity.status(HttpStatus.OK).body(testStepService.getTestStepsListResponse(testStepsList));
        }
    }

    @PostMapping("")
    public ResponseEntity<Object> saveTestSteps(@RequestBody List<TestStepInfoDTO> testStepInfoDTOList) {
        if (testStepInfoDTOList == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("TestSteps Info Not Found inside body.\n");
        }
        try {
            List<TestSteps> existingList = testStepService.getAllTestStepsFromTestCase(testStepInfoDTOList.get(0).getTestCaseId());
            List<TestSteps> testStepsList = new ArrayList<>();
            for (TestStepInfoDTO testStepInfoDTO : testStepInfoDTOList) {
                if (testStepInfoDTO.getStepId() == null || testStepInfoDTO.getStepId() == 0) {
                    testStepsList.add(testStepService.saveTestSteps(testStepInfoDTO));
                } else {
                    testStepsList.add(testStepService.editTestSteps(testStepInfoDTO, testStepInfoDTO.getStepId()));
                }
            }
            existingList.removeAll(testStepsList);
            testStepService.deleteAllTestSteps(existingList);
            return ResponseEntity.status(HttpStatus.OK).body(testStepsList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CustomResponseMessage(new Date(), "Error", e.getCause().getCause().getLocalizedMessage()));
        }
    }

    @PutMapping("/{testStepId}")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> editTestStep(@RequestBody TestStepInfoDTO testStepInfoDTO, @PathVariable int testStepId) {
        if (testStepInfoDTO == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("TestSteps Info Not Found inside body.\n");
        }
        try {
            if (testStepInfoDTO.getTestStepDesc() == null || testStepInfoDTO.getTestCaseId() == 0 || testStepInfoDTO.getTestExpectedOutput() == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("TestStep required info Not Found inside body.\n" + testStepInfoDTO);
            }
            TestSteps testSteps = testStepService.editTestSteps(testStepInfoDTO, testStepId);
            if (testSteps == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("TestStep Not found in database.\n" + testStepInfoDTO);
            }
            return ResponseEntity.status(HttpStatus.OK).body(testSteps);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CustomResponseMessage(new Date(), "Error", e.getCause().getCause().getLocalizedMessage()));
        }
    }

    @GetMapping("/order/{testCaseId}")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> orderTestSteps(@PathVariable int testCaseId) {
        TestCase testCase = testCaseRepository.findById(testCaseId);
        if (testCase == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Testcase Not Found for given id.\n");
        }
        try {
            List<TestSteps> testStepsList = testStepService.orderTestSteps(testCase);
            if (testStepsList == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("TestSteps Not Found for given testcaseid.\n");
            }
            return ResponseEntity.status(HttpStatus.OK).body(testStepsList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CustomResponseMessage(new Date(), "Error", e.getCause().getCause().getLocalizedMessage()));
        }
    }

    @PutMapping("/updateOrder")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> editTestStepOrder(@RequestBody List<TestStepOrderDTO> testStepOrderDTO) {
        List<TestSteps> testStepsList = new ArrayList<>();
        try {
            for (TestStepOrderDTO temp : testStepOrderDTO) {
                TestSteps testSteps = testStepsRepository.findById(temp.getTestStepId());
                if (testSteps == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("TestStep Not Found for given id.\n");
                }
                testSteps.setTestStepOrder(temp.getTestStepOrder());
                testStepsRepository.save(testSteps);
                testStepsList.add(testSteps);
            }
            this.orderTestSteps(testStepsList.get(0).getTestCase().getId());
            return ResponseEntity.status(HttpStatus.OK).body(testStepsRepository.findTestStepsByTestCaseOrderByTestStepOrderAsc(testStepsList.get(0).getTestCase()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CustomResponseMessage(new Date(), "Error", e.getCause().getCause().getLocalizedMessage()));
        }
    }

    @DeleteMapping("/{testStepId}")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> deleteTestStep(@PathVariable String testStepId) {
        if (testStepId == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("testStepId not found for delete operation.\n");
        }
        TestSteps testSteps = testStepsRepository.findById(Integer.parseInt(testStepId));
        if (testSteps == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new CustomResponseMessage(new Date(), "Error", "Provided testStep not found for Delete operation!"));
        }
        testStepsRepository.deleteById(Integer.parseInt(testStepId));
        return ResponseEntity.status(HttpStatus.OK).body(new CustomResponseMessage(new Date(), "Success", "TestStep Deleted Successfully!"));
    }
}
