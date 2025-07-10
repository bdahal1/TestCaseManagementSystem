package com.tcms.repositories;

import com.tcms.enums.ExecutionStatus;
import com.tcms.models.TestExecutions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestExecutionRepository extends PagingAndSortingRepository<TestExecutions, Integer>, JpaSpecificationExecutor<TestExecutions> {

    Page<TestExecutions> findAll(Pageable pageable);

    TestExecutions findById(int tagId);

    Page<TestExecutions> findByExecutionNameContaining(String executionName, Pageable pageable);

    void deleteById(int id);

    TestExecutions save(TestExecutions testExecutions);

    Page<TestExecutions> findByProjectsIdAndExecutionStatus(int projectId, ExecutionStatus executionStatus, Pageable pageable);
}
