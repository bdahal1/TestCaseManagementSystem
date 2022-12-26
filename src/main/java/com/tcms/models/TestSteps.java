package com.tcms.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "test_steps")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "testStepsSet"})
@AllArgsConstructor
@NoArgsConstructor
public class TestSteps {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ts_id", nullable = false)
    private Integer id;

    @Column(name = "ts_step_desc", nullable = false)
    private String testStepDesc;

    @Column(name = "ts_expected_output", nullable = false)
    private String testExpectedOutput;

    @Column(name = "ts_remarks")
    private String testRemarks;

    @Column(name = "ts_created_by", nullable = false)
    private String testCreatedBy;

    @Column(name = "ts_created_date", nullable = false)
    private Timestamp testCreatedDate;

    @Column(name = "ts_modified_by", nullable = false)
    private String testModifiedBy;

    @Column(name = "ts_modified_date", nullable = false)
    private Timestamp testModifiedDate;

    @Column(name = "ts_order", nullable = false)
    private Integer testStepOrder;

    @ManyToOne(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.PERSIST}, fetch = FetchType.LAZY)
    @JoinTable(name = "case_steps", joinColumns = {@JoinColumn(name = "step_id", referencedColumnName = "ts_id")}, inverseJoinColumns = {@JoinColumn(name = "case_id", referencedColumnName = "tc_id")})
    private TestCase testCase;


}
