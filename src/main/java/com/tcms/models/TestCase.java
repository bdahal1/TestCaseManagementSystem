package com.tcms.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Date;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "test_case")
@JsonIgnoreProperties("testStepsSet")
public class TestCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tc_id", nullable = false)
    private int id;

    @Column(name = "tc_name", nullable = false)
    private String testName;

    @Column(name = "tc_created_by", nullable = false)
    private String testCreatedBy;

    @Column(name = "tc_created_date", nullable = false)
    private Date testCreatedDate;

    @Column(name = "tc_modified_by", nullable = false)
    private String testModifiedBy;

    @Column(name = "tc_modified_date", nullable = false)
    private Date testModifiedDate;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "case_steps",
            joinColumns = {@JoinColumn(name = "case_id", referencedColumnName = "tc_id")},
            inverseJoinColumns = {@JoinColumn(name = "step_id", referencedColumnName = "ts_id")})
    private Set<TestSteps> testStepsSet;
}
