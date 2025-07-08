package com.tcms.repositories;

import com.tcms.models.Projects;
import com.tcms.models.TestCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface TestCaseRepository extends PagingAndSortingRepository<TestCase, Integer>, JpaSpecificationExecutor<TestCase> {

    Page<TestCase> findAll(Pageable pageable);

    Page<TestCase> findByProjectsIn(Set<Projects> projectsSet, Pageable pageable);

    List<TestCase> findByProjectsIn(Set<Projects> projectsSet);

    Page<TestCase> findByTestNameContaining(String testCaseName, Pageable pageable);

    TestCase findById(int id);

    void deleteById(int tcId);

    TestCase save(TestCase testCase);
}
