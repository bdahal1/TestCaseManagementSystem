package com.tcms.automation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AutomationJobRepository extends JpaRepository<AutomationJob, Long> {
    List<AutomationJob> findByCaseExecutionIdOrderByIdDesc(Integer caseExecutionId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM AutomationJob j WHERE j.projectId = :projectId AND (j.jobStatus = 'QUEUED' OR (j.jobStatus = 'RUNNING' AND j.leaseExpiration < :now)) ORDER BY j.id ASC")
    List<AutomationJob> findNextEligibleJobs(@Param("projectId") int projectId, @Param("now") Instant now, Pageable pageable);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from AutomationJob j where j.id = :id")
    Optional<AutomationJob> findForUpdate(@Param("id") Long id);

    boolean existsByCaseExecutionIdAndJobStatusIn(Integer caseExecutionId, java.util.Collection<JobStatus> statuses);

    Optional<AutomationJob> findFirstByCaseExecutionIdOrderByIdDesc(Integer caseExecutionId);

    List<AutomationJob> findByBatchIdOrderByIdAsc(Long batchId);

    @Query("select j.id from AutomationJob j where j.batchId = :batchId order by j.id")
    List<Long> findIdsByBatchId(@Param("batchId") Long batchId);

    List<AutomationJob> findByBatchIdAndJobStatusIn(Long batchId, java.util.Collection<JobStatus> statuses);

    // ---- History queries ----

    /**
     * Paginated history for a project with optional filters.
     * caseNameFragment is matched case-insensitively against the frozen caseName stored
     * inside the testDefinition JSON using LOCATE (portable across H2 and MySQL).
     */
    @Query("""
            SELECT j FROM AutomationJob j
            WHERE j.projectId = :projectId
              AND (:planId        IS NULL OR j.planId    = :planId)
              AND (:status        IS NULL OR j.jobStatus = :status)
              AND (:from          IS NULL OR j.createdAt >= :from)
              AND (:to            IS NULL OR j.createdAt <= :to)
              AND (:nameFragment  IS NULL OR LOWER(COALESCE(j.caseName, j.caseExecution.testCase.testName)) LIKE :nameFragment)
            ORDER BY j.id DESC
            """)
    Page<AutomationJob> findHistory(
            @Param("projectId")    int projectId,
            @Param("planId")       Integer planId,
            @Param("status")       JobStatus status,
            @Param("from")         Instant from,
            @Param("to")           Instant to,
            @Param("nameFragment") String nameFragment,
            Pageable pageable);

    /**
     * Returns all job IDs for a given case+plan pair, ordered ascending (oldest first).
     * Used by the UI attempt-picker to let users navigate to previous attempts.
     */
    @Query("""
            SELECT j.id FROM AutomationJob j
            WHERE j.planId  = :planId
              AND j.caseId  = :caseId
              AND j.projectId = :projectId
            ORDER BY j.id ASC
            """)
    List<Long> findAttemptIds(
            @Param("projectId") int projectId,
            @Param("planId")    int planId,
            @Param("caseId")    int caseId);

    List<AutomationJob> findByPlanIdAndCaseIdAndProjectIdOrderByIdAsc(Integer planId, Integer caseId, Integer projectId);
}

