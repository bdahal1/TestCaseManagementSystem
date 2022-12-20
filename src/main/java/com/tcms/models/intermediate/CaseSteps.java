package com.tcms.models.intermediate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "case_steps")
@AllArgsConstructor
@NoArgsConstructor
public class CaseSteps {
    @Id
    @Column(name = "case_id", nullable = false)
    private int caseId;

    @Column(name = "step_id", nullable = false)
    private int stepId;
}
