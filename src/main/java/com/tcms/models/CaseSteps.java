package com.tcms.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.NonNull;

@Entity
@Getter
@Setter
@Table(name = "case_steps")
public class CaseSteps {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cs_id", nullable = false)
    private int id;

    @Column(name = "case_id", nullable = false)
    private int caseId;

    @Column(name = "step_id", nullable = false)
    @NonNull
    private int stepId;
}
