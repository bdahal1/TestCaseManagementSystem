package com.tcms.models.intermediate;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "project_tests")
public class ProjectTests {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pt_id", nullable = false)
    private int id;

    @Column(name = "case_id", nullable = false)
    private int caseId;

    @Column(name = "pro_id", nullable = false)
    private int projectId;
}
