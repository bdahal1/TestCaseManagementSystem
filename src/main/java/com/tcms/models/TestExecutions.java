package com.tcms.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tcms.enums.ExecutionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "test_executions", uniqueConstraints = @UniqueConstraint(columnNames = {"execution_name", "project_id"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TestExecutions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "execution_id", nullable = false)
    private Integer id;

    @Column(name = "execution_name", nullable = false)
    private String executionName;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_status", nullable = false)
    private ExecutionStatus executionStatus=ExecutionStatus.IN_PROGRESS;

    // Nullable additions preserve existing executions when upgrading a database.
    private Boolean releasePlan = false;
    @Column(length=100) private String releaseName;
    @Column(length=100) private String buildName;
    @Column(length=100) private String environment;
    private java.time.LocalDate dueDate;
    @Column(length=4000) private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @JsonIgnore
    private Projects projects;

    @OneToMany(mappedBy = "testExecutions", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<TestCaseExecutions> testCaseExecutions = new HashSet<>();
}
