package com.tcms.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "test_case")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "testStepsSet"})
@AllArgsConstructor
@NoArgsConstructor
public class TestCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tc_id", nullable = false)
    private Integer id;

    @Column(name = "tc_name", nullable = false)
    private String testName;

    @Column(name = "tc_created_by", nullable = false)
    private String testCreatedBy;

    @Column(name = "tc_created_date", nullable = false)
    private java.sql.Timestamp testCreatedDate;

    @Column(name = "tc_modified_by", nullable = false)
    private String testModifiedBy;

    @Column(name = "tc_modified_date", nullable = false)
    private Timestamp testModifiedDate;

    @OneToMany(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.PERSIST}, fetch = FetchType.LAZY)
    @JoinTable(name = "case_steps", joinColumns = {@JoinColumn(name = "case_id", referencedColumnName = "tc_id")}, inverseJoinColumns = {@JoinColumn(name = "step_id", referencedColumnName = "ts_id")})
    private Set<TestSteps> testStepsSet;

    @ManyToOne(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.PERSIST}, fetch = FetchType.LAZY)
    @JoinTable(name = "project_tests", joinColumns = {@JoinColumn(name = "case_id", referencedColumnName = "tc_id")}, inverseJoinColumns = {@JoinColumn(name = "pro_id", referencedColumnName = "pro_id")})
    @JsonIgnoreProperties("testCaseSet")
    private Projects projects;
}
