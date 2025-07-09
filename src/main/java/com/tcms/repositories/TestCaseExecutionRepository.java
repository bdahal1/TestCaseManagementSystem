package com.tcms.repositories;

import com.tcms.models.TestCaseExecutions;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TestCaseExecutionRepository extends PagingAndSortingRepository<TestCaseExecutions, Integer>, JpaSpecificationExecutor<TestCaseExecutions> {

    Optional<TestCaseExecutions> findByTestExecutionsIdAndTestCaseId(Integer executionId, Integer testCaseId);

    TestCaseExecutions save(TestCaseExecutions tce);
}
