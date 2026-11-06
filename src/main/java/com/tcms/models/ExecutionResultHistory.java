package com.tcms.models;

import com.tcms.enums.TestResult;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Getter @Setter @NoArgsConstructor
@Table(name="execution_result_history")
public class ExecutionResultHistory {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="case_execution_id", nullable=false)
    private TestCaseExecutions caseExecution;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private TestResult status;
    @Column(length=4000) private String comment;
    @Lob @Column(length=10000) private String actualResult;
    @Column(length=2000) private String defectUrl;
    @Column(nullable=false) private Instant recordedAt;
    @Column(nullable=false) private String recordedBy;
}
