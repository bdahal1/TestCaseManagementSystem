package com.tcms.automation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;

public interface AutomationJobPreviewRepository extends JpaRepository<AutomationJobPreview, Long> {
    @Modifying
    @Query("delete from AutomationJobPreview p where p.updatedAt < :cutoff")
    void deleteExpired(Instant cutoff);
}
