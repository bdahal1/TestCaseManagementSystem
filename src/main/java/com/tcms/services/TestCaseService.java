package com.tcms.services;

import com.tcms.dto.TestCaseInfoDTO;
import com.tcms.helper.util.Util;
import com.tcms.models.Projects;
import com.tcms.models.Tags;
import com.tcms.models.TestCase;
import com.tcms.models.Users;
import com.tcms.repositories.ProjectRepository;
import com.tcms.repositories.TagsRepository;
import com.tcms.repositories.TestCaseRepository;
import com.tcms.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TestCaseService {

    private void details(TestCase testCase, TestCaseInfoDTO request) {
        if (request.getPriority()!=null) testCase.setPriority(request.getPriority());
        if (request.getAutomationStatus()!=null) testCase.setAutomationStatus(request.getAutomationStatus());
        if (request.getComponent()!=null) testCase.setComponent(request.getComponent().trim());
        if (request.getDescription()!=null) testCase.setDescription(request.getDescription());
        if (request.getPreconditions()!=null) testCase.setPreconditions(request.getPreconditions());
        if (request.getEstimatedMinutes()!=null) testCase.setEstimatedMinutes(request.getEstimatedMinutes()==0 ? null : request.getEstimatedMinutes());
        if (request.getCustomFields()!=null) {
            var cleaned=new LinkedHashMap<String,String>();
            for (Map.Entry<String, String> field : request.getCustomFields().entrySet()) {
                String fieldName = field.getKey().trim();
                if (cleaned.containsKey(fieldName)) {
                    throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,"Custom field names must be unique");
                }
                cleaned.put(fieldName, field.getValue());
            }
            testCase.setCustomFields(cleaned);
        }
        if (request.getOwnerId()!=null) {
            if (request.getOwnerId()==0) { testCase.setOwnerId(null); return; }
            Users owner=userRepository.findById(request.getOwnerId().intValue());
            if (owner==null || !Boolean.TRUE.equals(owner.getIsActive()) || owner.getProjectsSet()==null
                    || owner.getProjectsSet().stream().noneMatch(p -> p.getId().equals(testCase.getProjects().getId())))
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose an active owner assigned to this project");
            testCase.setOwnerId(owner.getId());
        }
    }

    public final TestCaseRepository testCaseRepository;
    public final ProjectRepository projectRepository;
    public final TagsRepository tagsRepository;
    public final UserRepository userRepository;
    private final GherkinDocuments gherkinDocuments;

    public TestCaseService(TestCaseRepository testCaseRepository, ProjectRepository projectRepository, TagsRepository tagsRepository, UserRepository userRepository, GherkinDocuments gherkinDocuments) {
        this.gherkinDocuments=gherkinDocuments;
        this.testCaseRepository = testCaseRepository;
        this.projectRepository = projectRepository;
        this.tagsRepository = tagsRepository;
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
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Object> saveTestCase(TestCaseInfoDTO testCaseInfoDTO) {
        TestCase testCase = new TestCase();
        testCase.setTestName(testCaseInfoDTO.getTestName());
        Users user = userRepository.findByUserName(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName());
        String fullName = user.getFirstName() + " " + user.getLastName();
        testCase.setTestCreatedBy(fullName);
        testCase.setTestCreatedDate(Util.parseTimestamp(Util.DATE_TIME_FORMAT.format(new Date())));
        testCase.setTestModifiedBy(fullName);
        testCase.setTestModifiedDate(Util.parseTimestamp(Util.DATE_TIME_FORMAT.format(new Date())));
        testCase.setTestType(testCaseInfoDTO.getTestType());
        if (testCaseInfoDTO.getSelectedTags() != null) {
            Set<Tags> tagsList = new HashSet<>();
            for (Integer tagId : Arrays.asList(testCaseInfoDTO.getSelectedTags())) {
                Tags tag = tagsRepository.findById(tagId);
                if (tag != null) {
                    tagsList.add(tag);
                }
            }
            testCase.setTagsSet(tagsList);
        }
        Projects project = projectRepository.findById(testCaseInfoDTO.getProjectId());
        testCase.setProjects(project);
        testCase.setTestProjectId(project.getProjectInitials()+ "-" +(project.getIssueCounter() + 1));
        project.setIssueCounter(project.getIssueCounter() + 1);
        details(testCase,testCaseInfoDTO);
        if (testCaseInfoDTO.getGherkin()!=null) gherkinDocuments.replace(testCase,testCaseInfoDTO.getGherkin(),fullName);
        testCaseRepository.save(testCase);
        return ResponseEntity.status(HttpStatus.OK).body(testCase);
    }

    @SuppressWarnings("Duplicates")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Object> editTestCase(TestCaseInfoDTO testCaseInfoDTO, int testCaseId) {
        TestCase testCaseEdit = testCaseRepository.findById(testCaseId);
        if (testCaseEdit == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Testcase Not Found in database.\n");
        }
        testCaseEdit.setTestName(testCaseInfoDTO.getTestName() == null ? testCaseEdit.getTestName() : testCaseInfoDTO.getTestName());
        Users user = userRepository.findByUserName(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName());
        String fullName = user.getFirstName() + " " + user.getLastName();
        testCaseEdit.setTestModifiedBy(fullName);
        testCaseEdit.setTestType(testCaseInfoDTO.getTestType()==null ? testCaseEdit.getTestType() : testCaseInfoDTO.getTestType());
        testCaseEdit.setTestModifiedDate(Util.parseTimestamp(Util.DATE_TIME_FORMAT.format(new Date())));
        if (testCaseInfoDTO.getSelectedTags() != null) {
            Set<Tags> tagsList = new HashSet<>();
            for (Integer tagId : Arrays.asList(testCaseInfoDTO.getSelectedTags())) {
                Tags tag = tagsRepository.findById(tagId);
                if (tag != null) {
                    tagsList.add(tag);
                }
            }
            testCaseEdit.setTagsSet(tagsList);
        }
        details(testCaseEdit,testCaseInfoDTO);
        if (testCaseInfoDTO.getGherkin()!=null) gherkinDocuments.replace(testCaseEdit,testCaseInfoDTO.getGherkin(),fullName);
        testCaseRepository.save(testCaseEdit);
        return ResponseEntity.status(HttpStatus.OK).body(testCaseEdit);
    }
}
