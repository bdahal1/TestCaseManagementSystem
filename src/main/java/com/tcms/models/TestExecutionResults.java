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
@Table(name = "test_execution_result")
public class TestExecutionResults {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id", nullable = false)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false)
    private TestResult resultStatus;

    @Column(name = "result_comment", nullable = false)
    private String resultComment;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tce_id", nullable = false, unique = true)
    @JsonIgnore
    private TestCaseExecutions testCaseExecution;
}
