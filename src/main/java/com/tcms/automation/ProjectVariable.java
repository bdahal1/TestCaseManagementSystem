package com.tcms.automation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tcms.models.Projects;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "project_variables", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_id", "var_key"})
})
public class ProjectVariable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @JsonIgnore
    private Projects project;

    @Column(name = "var_key", nullable = false, length = 120)
    private String variableKey;

    @Column(name = "var_value", length = 2000)
    private String variableValue;

    @Column(name = "is_secret", nullable = false)
    private Boolean isSecret = false;

    @Column(name = "description", length = 500)
    private String description;
}
