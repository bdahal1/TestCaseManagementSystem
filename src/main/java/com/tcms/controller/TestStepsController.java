package com.tcms.controller;

import com.tcms.dto.TestStepInfoDTO;
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
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> saveTestSteps(@RequestBody List<TestStepInfoDTO> testStepInfoDTO) {
        if (testStepInfoDTO == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("TestSteps Info Not Found inside body.\n");
        }
        try {
            List<TestSteps> testStepsList = new ArrayList<>();
            for (TestStepInfoDTO each : testStepInfoDTO) {
                if (each.getTestStepDesc() == null || each.getTestCaseId() == 0 || each.getTestExpectedOutput() == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("TestStep required info Not Found inside body.\n" + each);
                }
                Users user = userRepository.findById(each.getUserId());
                String fullName = user.getFirstName() + " " + user.getLastName();
                TestSteps testSteps = new TestSteps();
                testSteps.setTestCase(testCaseRepository.findById(each.getTestCaseId()));
                testSteps.setTestRemarks(each.getTestRemarks());
                testSteps.setTestStepDesc(each.getTestStepDesc());
                testSteps.setTestExpectedOutput(each.getTestExpectedOutput());
                testSteps.setTestStepOrder(each.getTestStepOrder());
                testSteps.setTestCreatedBy(fullName);
                testSteps.setTestCreatedDate(Util.parseTimestamp(Util.DATE_TIME_FORMAT.format(new Date())));
                testSteps.setTestModifiedBy(fullName);
                testSteps.setTestModifiedDate(Util.parseTimestamp(Util.DATE_TIME_FORMAT.format(new Date())));
                testStepsList.add(testSteps);
            }
            if (testStepsList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CustomResponseMessage(new Date(), "Error", "Teststeps cannot be added. Please review and try again!!"));
            }
            testStepsRepository.save(testStepsList);
            return ResponseEntity.status(HttpStatus.OK).body(testStepsList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CustomResponseMessage(new Date(), "Error", e.getCause().getCause().getLocalizedMessage()));
        }
    }

    @PutMapping("/{testStepId}")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> editTestStep(@RequestBody TestSteps testSteps, @PathVariable int testStepId) {
        //TODO
        return null;
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
