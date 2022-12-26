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
@Table(name = "department")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "usersSet"})
@AllArgsConstructor
@NoArgsConstructor
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dep_id", nullable = false)
    private Integer depId;

    @Column(name = "dep_name", nullable = false, unique = true)
    private String depName;

    @OneToMany(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.PERSIST}, fetch = FetchType.LAZY)
    @JoinTable(name = "users_department", joinColumns = {@JoinColumn(name = "dep_id", referencedColumnName = "dep_id")}, inverseJoinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "u_id")})
    @JsonIgnoreProperties("department")
    private Set<Users> usersSet;

}
