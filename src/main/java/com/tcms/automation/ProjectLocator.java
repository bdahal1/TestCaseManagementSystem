package com.tcms.automation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tcms.models.Projects;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "project_locators", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_id", "locator_name"})
})
public class ProjectLocator {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @JsonIgnore
    private Projects project;

    @Column(name = "locator_name", nullable = false, length = 120)
    private String locatorName;

    @Enumerated(EnumType.STRING)
    @Column(name = "locator_type", nullable = false, length = 30)
    private LocatorType locatorType;

    @Column(name = "locator_value", nullable = false, length = 1000)
    private String locatorValue;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_date")
    private Timestamp createdDate;

    @Column(name = "modified_by")
    private String modifiedBy;

    @Column(name = "modified_date")
    private Timestamp modifiedDate;
}
