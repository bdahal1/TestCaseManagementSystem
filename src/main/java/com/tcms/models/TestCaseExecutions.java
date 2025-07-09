package com.tcms.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

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

    // Join to test_executions
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_execution_id", referencedColumnName = "execution_id", nullable = false)
    @JsonIgnore
    private TestExecutions testExecutions;

    // Join to test_case
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", referencedColumnName = "tc_id", nullable = false)
    private TestCase testCase;

    // Join to test_execution_result
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "result_id", referencedColumnName = "result_id")
    private TestExecutionResults result;
}
