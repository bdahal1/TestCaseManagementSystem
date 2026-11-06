package com.tcms.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tcms.enums.TestResult;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "test_case_executions")
public class TestCaseExecutions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tce_id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_execution_id", nullable = false)
    @JsonIgnore
    private TestExecutions testExecutions;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private TestCase testCase;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status")
    private TestResult resultStatus;

    @Column(name = "result_comment", length=4000)
    private String resultComment;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="assigned_user_id") @JsonIgnore
    private Users assignee;
    @Lob @Column(length=1048576) @JsonIgnore
    private String caseSnapshot;
    @Lob @Column(length=10000)
    private String actualResult;
    @Column(length=2000)
    private String defectUrl;
    private Long resultRevision = 0L;
    private java.time.Instant recordedAt;
    private String recordedBy;
    @OneToMany(mappedBy="caseExecution", cascade=CascadeType.ALL, orphanRemoval=true) @JsonIgnore
    private java.util.List<ExecutionResultHistory> history = new java.util.ArrayList<>();
    @OneToMany(mappedBy="caseExecution", cascade=CascadeType.ALL, orphanRemoval=true) @JsonIgnore
    private java.util.List<ExecutionEvidence> evidence = new java.util.ArrayList<>();
}
