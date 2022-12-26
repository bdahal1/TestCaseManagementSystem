package com.tcms.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "project")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "usersSet"})
@AllArgsConstructor
@NoArgsConstructor
public class Projects {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pro_id", nullable = false)
    private Integer id;

    @Column(name = "pro_name", nullable = false, unique = true)
    private String projectName;

    @ManyToMany(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.PERSIST}, fetch = FetchType.LAZY)
    @JoinTable(name = "users_projects", joinColumns = {@JoinColumn(name = "pro_id", referencedColumnName = "pro_id")}, inverseJoinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "u_id")})
    private Set<Users> usersSet;

    @OneToMany(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.PERSIST}, fetch = FetchType.LAZY)
    @JoinTable(name = "project_tests", joinColumns = {@JoinColumn(name = "pro_id", referencedColumnName = "pro_id")}, inverseJoinColumns = {@JoinColumn(name = "case_id", referencedColumnName = "tc_id")})
    @JsonIgnoreProperties("projects")
    private Set<TestCase> testCaseSet;
}
