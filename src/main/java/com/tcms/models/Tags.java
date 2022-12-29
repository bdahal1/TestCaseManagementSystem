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
@Table(name = "tags")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "testCaseSet"})
@AllArgsConstructor
@NoArgsConstructor
public class Tags {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id", nullable = false)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String tagName;

    @ManyToMany(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.PERSIST}, fetch = FetchType.LAZY)
    @JoinTable(name = "test_case_tags", joinColumns = {@JoinColumn(name = "tag_case_id", referencedColumnName = "tag_id")}, inverseJoinColumns = {@JoinColumn(name = "case_id", referencedColumnName = "tc_id")})
    private Set<TestCase> testCaseSet;
}
