package com.tcms.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "test_executions", uniqueConstraints = {@UniqueConstraint(columnNames = {"execution_name", "project_id"})})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "testCaseSet"})
@AllArgsConstructor
@NoArgsConstructor
public class TestExecutions {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "execution_id", nullable = false)
    private Integer id;

    @Column(name = "execution_name", nullable = false)
    private String executionName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Projects projects;

    @OneToMany(mappedBy = "testExecutions", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"testCase", "testExecutions"})
    private Set<TestCaseExecutions> testCaseExecutions;
}
