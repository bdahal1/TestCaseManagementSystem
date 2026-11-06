package com.tcms.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Getter @Setter @NoArgsConstructor
@Table(name="execution_evidence")
public class ExecutionEvidence {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="case_execution_id", nullable=false)
    private TestCaseExecutions caseExecution;
    @Column(nullable=false, length=150) private String name;
    @Column(nullable=false, length=100) private String contentType;
    @Column(name="file_size", nullable=false) private long size;
    @Column(nullable=false) private Instant uploadedAt;
    @Column(nullable=false) private String uploadedBy;
    @Lob @Column(nullable=false, length=5242880) private byte[] content;
}
