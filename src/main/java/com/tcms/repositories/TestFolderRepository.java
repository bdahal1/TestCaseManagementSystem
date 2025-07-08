package com.tcms.repositories;

import com.tcms.models.TestFolders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestFolderRepository extends PagingAndSortingRepository<TestFolders, Integer>, JpaSpecificationExecutor<TestFolders> {

    Page<TestFolders> findAll(Pageable pageable);

    TestFolders findById(int tagId);

    Page<TestFolders> findByFolderNameContaining(String folderName, Pageable pageable);

    void deleteById(int id);

    TestFolders save(TestFolders testFolders);

    Page<TestFolders> findByProjectsId(int projectId, Pageable pageable);

    List<TestFolders> findByProjectsId(int projectId);

}
