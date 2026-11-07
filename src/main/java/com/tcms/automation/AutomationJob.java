package com.tcms.automation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tcms.enums.TestTypes;
import com.tcms.models.TestCaseExecutions;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "automation_jobs")
public class AutomationJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_execution_id", nullable = false)
    @JsonIgnore
    private TestCaseExecutions caseExecution;

    @Column(name = "plan_id", nullable = false)
    private Integer planId;

    @Column(name = "case_id", nullable = false)
    private Integer caseId;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_status", nullable = false, length = 30)
    private JobStatus jobStatus = JobStatus.QUEUED;

    @Enumerated(EnumType.STRING)
    @Column(name = "test_type", nullable = false, length = 30)
    private TestTypes testType;

    @JsonIgnore
    @Lob
    @Column(name = "test_definition", nullable = false, length = 1048576)
    private String testDefinition;

    // Old queued jobs have no revision/lease and cannot publish results after an upgrade.
    private Long expectedResultRevision;
    @JsonIgnore @Column(length=64)
    private String leaseTokenHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "assigned_worker_id")
    private String assignedWorkerId;

    @Column(name = "lease_expiration")
    private Instant leaseExpiration;

    @Column(name = "error_message", length = 4000)
    private String errorMessage;

    @Lob
    @Column(name = "step_results_json", length = 1048576)
    private String stepResultsJson;

    @Lob
    @Column(name = "diagnostic_logs", length = 1048576)
    private String diagnosticLogs;
}
