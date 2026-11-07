package com.tcms.automation;

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
}
