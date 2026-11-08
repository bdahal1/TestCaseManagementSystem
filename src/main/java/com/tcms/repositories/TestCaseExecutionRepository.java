package com.tcms.repositories;

import com.tcms.models.TestCaseExecutions;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TestCaseExecutionRepository extends PagingAndSortingRepository<TestCaseExecutions, Integer>, JpaSpecificationExecutor<TestCaseExecutions> {

    Optional<TestCaseExecutions> findByTestExecutionsIdAndTestCaseId(Integer executionId, Integer testCaseId);

    java.util.List<TestCaseExecutions> findByTestExecutionsId(Integer executionId);

    Optional<TestCaseExecutions> findById(Integer id);

    @org.springframework.data.jpa.repository.Query("select count(l) from TestCaseExecutions l where l.testCase.id = :caseId "
            + "and (l.testExecutions.releasePlan = true or l.history is not empty or l.evidence is not empty)")
    long countProtectedCaseReferences(@org.springframework.data.repository.query.Param("caseId") int caseId);

    TestCaseExecutions save(TestCaseExecutions tce);
}
