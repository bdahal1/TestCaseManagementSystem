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

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from TestExecutions e where e.id = :id")
    java.util.Optional<TestExecutions> findForUpdate(@org.springframework.data.repository.query.Param("id") int id);

    Page<TestExecutions> findByProjectsId(int projectId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("select e from TestExecutions e where e.projects.id = :projectId "
            + "and (:q = '' or locate(lower(:q), lower(e.executionName)) > 0 "
            + "or locate(lower(:q), lower(coalesce(e.releaseName, ''))) > 0 "
            + "or locate(lower(:q), lower(coalesce(e.buildName, ''))) > 0) "
            + "and (:assigneeId is null or exists (select l.id from TestCaseExecutions l where l.testExecutions = e and l.assignee.id = :assigneeId))")
    Page<TestExecutions> searchPlans(@org.springframework.data.repository.query.Param("projectId") int projectId,
            @org.springframework.data.repository.query.Param("q") String q,
            @org.springframework.data.repository.query.Param("assigneeId") Integer assigneeId, Pageable pageable);

    Page<TestExecutions> findAll(Pageable pageable);

    TestExecutions findById(int tagId);

    Page<TestExecutions> findByExecutionNameContaining(String executionName, Pageable pageable);

    void deleteById(int id);

    TestExecutions save(TestExecutions testExecutions);

    Page<TestExecutions> findByProjectsIdAndExecutionStatus(int projectId, ExecutionStatus executionStatus, Pageable pageable);
}
