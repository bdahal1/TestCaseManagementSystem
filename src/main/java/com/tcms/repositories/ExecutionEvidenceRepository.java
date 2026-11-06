package com.tcms.repositories;

import com.tcms.models.ExecutionEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface ExecutionEvidenceRepository extends JpaRepository<ExecutionEvidence, Integer> {
    interface Summary {
        Integer getId(); String getName(); String getContentType(); long getSize();
        Instant getUploadedAt(); String getUploadedBy();
    }
    List<Summary> findByCaseExecutionIdOrderByUploadedAtAsc(int caseExecutionId);
    long countByCaseExecutionId(int caseExecutionId);
}
