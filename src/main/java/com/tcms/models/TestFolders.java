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
@Table(name = "test_folders", uniqueConstraints = {@UniqueConstraint(columnNames = {"folder_name", "project_id"})})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "testCaseSet"})
@AllArgsConstructor
@NoArgsConstructor
public class TestFolders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "folder_id", nullable = false)
    private Integer id;

    @Column(name = "folder_name", nullable = false)
    private String folderName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Projects projects;

    @ManyToMany(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.PERSIST}, fetch = FetchType.LAZY)
    @JoinTable(name = "test_case_folders", joinColumns = {@JoinColumn(name = "test_folder_id", referencedColumnName = "folder_id")}, inverseJoinColumns = {@JoinColumn(name = "case_id", referencedColumnName = "tc_id")})
    private Set<TestCase> testCaseSet;
}
