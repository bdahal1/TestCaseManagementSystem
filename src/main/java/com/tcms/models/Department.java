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
@JsonIgnoreProperties("userDepartmentSet")
@AllArgsConstructor
@NoArgsConstructor
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dep_id", nullable = false)
    private int depId;

    @Column(name = "dep_name", nullable = false, unique = true)
    private String depName;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "users_department",
            joinColumns = {@JoinColumn(name = "dep_id", referencedColumnName = "dep_id")},
            inverseJoinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "u_id")})
    private Set<Users> userDepartmentSet;

}
