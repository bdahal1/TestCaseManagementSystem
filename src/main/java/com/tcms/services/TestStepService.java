package com.tcms.services;

import com.tcms.dto.TestStepInfoDTO;
import com.tcms.helper.util.Util;
import com.tcms.models.TestCase;
import com.tcms.models.TestSteps;
import com.tcms.models.Users;
import com.tcms.repositories.TestCaseRepository;
import com.tcms.repositories.TestStepsRepository;
import com.tcms.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TestStepService {
    public final UserRepository userRepository;
    public final TestCaseRepository testCaseRepository;

    public final TestStepsRepository testStepsRepository;

    public TestStepService(UserRepository userRepository, TestCaseRepository testCaseRepository, TestStepsRepository testStepsRepository) {
        this.userRepository = userRepository;
        this.testCaseRepository = testCaseRepository;
        this.testStepsRepository = testStepsRepository;
    }

    public Map<String, Object> getTestStepsListResponse(Page<TestSteps> testStepsList) {
        Map<String, Object> response = new HashMap<>();
        response.put("testSteps", testStepsList.getContent());
        response.put("currentPage", testStepsList.getNumber());
        response.put("totalItems", testStepsList.getTotalElements());
        response.put("totalPages", testStepsList.getTotalPages());
        return response;
    }

    public List<TestSteps> getAllTestStepsFromTestCase(Integer testCaseId) {
        TestCase testCase = testCaseRepository.findById(testCaseId);
        return testStepsRepository.findTestStepsByTestCaseOrderByTestStepOrderAsc(testCase);
    }

    public void deleteAllTestSteps(List<TestSteps> testSteps) {
        for (TestSteps testStep : testSteps) {
            testStepsRepository.deleteById(testStep.getId());
        }
    }

    @SuppressWarnings("Duplicates")
    public TestSteps saveTestSteps(TestStepInfoDTO testStepInfoDTO) {
        Users user = userRepository.findById(testStepInfoDTO.getUserId());
        String fullName = user.getFirstName() + " " + user.getLastName();
        TestSteps testSteps = new TestSteps();
        testSteps.setTestCase(testCaseRepository.findById(testStepInfoDTO.getTestCaseId()));
        testSteps.setTestStepData(testStepInfoDTO.getTestRemarks());
        testSteps.setTestStepDesc(testStepInfoDTO.getTestStepDesc());
        testSteps.setTestExpectedOutput(testStepInfoDTO.getTestExpectedOutput());
        TestSteps newOrder = testStepsRepository.findFirstByTestCaseOrderByTestStepOrderDesc(testCaseRepository.findById(testStepInfoDTO.getTestCaseId()));
        testSteps.setTestStepOrder(newOrder == null ? 1 : newOrder.getTestStepOrder() + 1);
        testSteps.setTsCreatedBy(fullName);
        testSteps.setTsCreatedDate(Util.parseTimestamp(Util.DATE_TIME_FORMAT.format(new Date())));
        testSteps.setTsModifiedBy(fullName);
        testSteps.setTsModifiedDate(Util.parseTimestamp(Util.DATE_TIME_FORMAT.format(new Date())));
        testStepsRepository.save(testSteps);
        return testSteps;
    }

    @SuppressWarnings("Duplicates")
    public TestSteps editTestSteps(TestStepInfoDTO testStepInfoDTO, int testStepId) {
        Users user = userRepository.findById(testStepInfoDTO.getUserId());
        String fullName = user.getFirstName() + " " + user.getLastName();
        TestSteps testSteps = testStepsRepository.findById(testStepId);
        if (testSteps == null) {
            return null;
        }
        testSteps.setTestCase(testSteps.getTestCase());
        testSteps.setTestStepData(testStepInfoDTO.getTestRemarks() == null ? testSteps.getTestRemarks() : testStepInfoDTO.getTestRemarks());
        testSteps.setTestStepDesc(testStepInfoDTO.getTestStepDesc() == null ? testSteps.getTestStepDesc() : testStepInfoDTO.getTestStepDesc());
        testSteps.setTestExpectedOutput(testStepInfoDTO.getTestExpectedOutput() == null ? testSteps.getTestExpectedOutput() : testStepInfoDTO.getTestExpectedOutput());
        testSteps.setTestStepOrder(testSteps.getTestStepOrder());
        testSteps.setTsCreatedBy(testSteps.getTsCreatedBy());
        testSteps.setTsCreatedDate(testSteps.getTsCreatedDate());
        testSteps.setTsModifiedBy(fullName);
        testSteps.setTsModifiedDate(Util.parseTimestamp(Util.DATE_TIME_FORMAT.format(new Date())));
        testStepsRepository.save(testSteps);
        return testSteps;
    }

    public List<TestSteps> orderTestSteps(TestCase testCase) {
        List<TestSteps> testStepsList = testStepsRepository.findTestStepsByTestCaseOrderByTestStepOrderAsc(testCase);
        if (testStepsList.size() == 0) {
            return null;
        }
        int newOrder = 1;
        for (TestSteps each : testStepsList) {
            each.setTestStepOrder(newOrder);
            testStepsRepository.save(each);
            newOrder++;
        }
        return testStepsList;
    }
}
