package com.tcms.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.NonNull;

import java.sql.Date;

@Entity
@Getter
@Setter
@Table(name="test_case")
public class TestCase {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "tc_id")
    @NonNull
    private int id;

    @Column(name = "tc_name")
    @NonNull
    private String testName;

    @Column(name = "tc_created_by")
    @NonNull
    private String testCreatedBy;

    @Column(name = "tc_created_date")
    @NonNull
    private Date testCreatedDate;

    @Column(name = "tc_modified_by")
    @NonNull
    private String testModifiedBy;

    @Column(name = "tc_modified_date")
    @NonNull
    private Date testModifiedDate;

    @Override
    public String toString() {
        return "TestCase{" +
                "id=" + id +
                ", testName='" + testName + '\'' +
                ", testCreatedBy='" + testCreatedBy + '\'' +
                ", testCreatedDate=" + testCreatedDate +
                ", testModifiedBy='" + testModifiedBy + '\'' +
                ", testModifiedDate=" + testModifiedDate +
                '}';
    }
}
