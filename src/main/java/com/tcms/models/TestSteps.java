package com.tcms.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.NonNull;

import java.sql.Date;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name="test_steps")
@JsonIgnoreProperties("testCase")
public class TestSteps {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ts_id")
    @NonNull
    private int id;

    @Column(name = "ts_step_desc")
    @NonNull
    private String testStepDesc;

    @Column(name = "ts_expected_output")
    @NonNull
    private String testExpectedOutput;

    @Column(name = "ts_remarks")
    private String testRemarks;

    @Column(name = "ts_created_by")
    @NonNull
    private String testCreatedBy;

    @Column(name = "ts_created_date")
    @NonNull
    private Date testCreatedDate;

    @Column(name = "ts_modified_by")
    @NonNull
    private String testModifiedBy;

    @Column(name = "ts_modified_date")
    @NonNull
    private Date testModifiedDate;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "case_steps",
            joinColumns = {@JoinColumn(name = "step_id", referencedColumnName = "ts_id")},
            inverseJoinColumns = {@JoinColumn(name = "case_id", referencedColumnName = "tc_id")})
    private TestCase testCase;


}
