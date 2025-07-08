package com.tcms.repositories;

import com.tcms.models.TestCase;
import com.tcms.models.TestSteps;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestStepsRepository extends PagingAndSortingRepository<TestSteps, Integer>, JpaSpecificationExecutor<TestSteps> {

    Page<TestSteps> findAll(Pageable pageable);

    List<TestSteps> findTestStepsByTestCaseOrderByTestStepOrderAsc(TestCase testCase);

    Page<TestSteps> findAllByTestCase(TestCase testCase, Pageable pageable);

    TestSteps findById(int id);

    void deleteById(int tcId);

    TestSteps save(TestSteps testSteps);

    TestSteps findFirstByTestCaseOrderByTestStepOrderDesc(TestCase testCase);
}
