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
@Table(name = "test_case_tags")
@AllArgsConstructor
@NoArgsConstructor
public class TestCaseTags {
    @Id
    @Column(name = "case_id", nullable = false)
    private Integer caseId;

    @Column(name = "tag_case_id",nullable = false)
    private Integer tagCaseId;
}
