package com.tcms.automation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface AutomationBatchRepository extends JpaRepository<AutomationBatch, Long> {
    Optional<AutomationBatch> findFirstByPlanIdOrderByIdDesc(Integer planId);

    List<AutomationBatch> findByPlanIdOrderByIdDesc(Integer planId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM AutomationBatch b WHERE b.id = :id")
    Optional<AutomationBatch> findForUpdate(@Param("id") Long id);
}
