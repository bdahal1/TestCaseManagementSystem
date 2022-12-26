package com.tcms.services;

import com.tcms.dto.TestCaseInfoDTO;
import com.tcms.helper.util.Util;
import com.tcms.models.TestCase;
import com.tcms.models.Users;
import com.tcms.repositories.ProjectRepository;
import com.tcms.repositories.TestCaseRepository;
import com.tcms.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class TestCaseService {

    public final TestCaseRepository testCaseRepository;
    public final ProjectRepository projectRepository;

    public final UserRepository userRepository;

    public TestCaseService(TestCaseRepository testCaseRepository, ProjectRepository projectRepository, UserRepository userRepository) {
        this.testCaseRepository = testCaseRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }


    public Map<String, Object> getTestCaseListResponse(Page<TestCase> testCaseList) {
        Map<String, Object> response = new HashMap<>();
        response.put("testCase", testCaseList.getContent());
        response.put("currentPage", testCaseList.getNumber());
        response.put("totalItems", testCaseList.getTotalElements());
        response.put("totalPages", testCaseList.getTotalPages());
        return response;
    }

    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> saveTestCase(TestCaseInfoDTO testCaseInfoDTO) {
        TestCase testCase = new TestCase();
        testCase.setTestName(testCaseInfoDTO.getTestName());
        Users user = userRepository.findById(testCaseInfoDTO.getUserId());
        String fullName = user.getFirstName() + " " + user.getLastName();
        testCase.setTestCreatedBy(fullName);
        testCase.setTestCreatedDate(Util.parseTimestamp(Util.DATE_TIME_FORMAT.format(new Date())));
        testCase.setTestModifiedBy(fullName);
        testCase.setTestModifiedDate(Util.parseTimestamp(Util.DATE_TIME_FORMAT.format(new Date())));
        if (testCaseInfoDTO.getProjectId() == 0) {
            testCase.setProjects(null);
        } else {
            testCase.setProjects(projectRepository.findById(testCaseInfoDTO.getProjectId()));
        }
        testCaseRepository.save(testCase);
        return ResponseEntity.status(HttpStatus.OK).body(testCase);
    }

    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> editTestCase(TestCaseInfoDTO testCaseInfoDTO, int testCaseId) {
        TestCase testCaseEdit = testCaseRepository.findById(testCaseId);
        if (testCaseEdit == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Testcase Not Found in database.\n");
        }
        testCaseEdit.setTestName(testCaseInfoDTO.getTestName() == null ? testCaseEdit.getTestName() : testCaseInfoDTO.getTestName());
        Users user = userRepository.findById(testCaseInfoDTO.getUserId());
        String fullName = user.getFirstName() + " " + user.getLastName();
        testCaseEdit.setTestModifiedBy(fullName);
        testCaseEdit.setTestModifiedDate(Util.parseTimestamp(Util.DATE_TIME_FORMAT.format(new Date())));
        testCaseRepository.save(testCaseEdit);
        return ResponseEntity.status(HttpStatus.OK).body(testCaseEdit);
    }
}
