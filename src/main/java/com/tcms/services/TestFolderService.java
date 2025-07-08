package com.tcms.services;

import com.tcms.models.TestFolders;
import com.tcms.repositories.TestFolderRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class TestFolderService {
    final public TestFolderRepository testFolderRepository;

    public TestFolderService(TestFolderRepository testFolderRepository) {
        this.testFolderRepository = testFolderRepository;
    }

    public Map<String, Object> getTestFolderListResponse(Page<TestFolders> testFoldersList) {
        Map<String, Object> response = new HashMap<>();
        response.put("testFolders", testFoldersList.getContent());
        response.put("currentPage", testFoldersList.getNumber());
        response.put("totalItems", testFoldersList.getTotalElements());
        response.put("totalPages", testFoldersList.getTotalPages());
        return response;
    }

    public TestFolders saveTestFolder(TestFolders testFolders) {
        return testFolderRepository.save(testFolders);
    }
}
