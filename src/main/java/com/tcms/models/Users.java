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
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "u_id")
    private Integer id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "u_name", nullable = false, unique = true)
    private String userName;

    @Column(name = "u_password", nullable = false)
    private String password;

    @Column(name = "u_is_active", nullable = false)
    private Boolean isActive = true;

    @ManyToMany(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.PERSIST}, fetch = FetchType.LAZY)
    @JoinTable(name = "users_projects", joinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "u_id")}, inverseJoinColumns = {@JoinColumn(name = "pro_id", referencedColumnName = "pro_id")})
    @JsonIgnoreProperties({"usersSet", "testCaseSet"})
    private Set<Projects> projectsSet;

    @ManyToMany(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.PERSIST}, fetch = FetchType.LAZY)
    @JoinTable(name = "users_roles", joinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "u_id")}, inverseJoinColumns = {@JoinColumn(name = "role_id", referencedColumnName = "roleId")})
    @JsonIgnoreProperties("usersSet")
    private Set<Roles> roleSet;

    @ManyToOne(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.PERSIST}, fetch = FetchType.LAZY)
    @JoinTable(name = "users_department", joinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "u_id")}, inverseJoinColumns = {@JoinColumn(name = "dep_id", referencedColumnName = "dep_id")})
    @JsonIgnoreProperties("usersSet")
    private Department department;
}
