package com.tcms.automation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Getter
@Setter
@Table(name = "automation_job_previews", indexes = @Index(name = "automation_preview_updated_idx", columnList = "updated_at"))
public class AutomationJobPreview {
    @Id
    private Long jobId;
    private long sequence;
    @Column(name = "updated_at")
    private Instant updatedAt;
    @Lob
    private String progressJson;
    @Lob
    @Column(length = 524288)
    private byte[] screenshot;
}
