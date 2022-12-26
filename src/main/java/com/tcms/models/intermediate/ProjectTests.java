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
@Table(name = "project_tests")
@AllArgsConstructor
@NoArgsConstructor
public class ProjectTests {
    @Id
    @Column(name = "case_id", nullable = false)
    private Integer caseId;

    @Column(name = "pro_id", nullable = false)
    private Integer projectId;
}
