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
@JsonIgnoreProperties({"projectsSet", "roleSet", "department"})
@AllArgsConstructor
@NoArgsConstructor
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "u_id")
    private int id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "u_name", nullable = false, unique = true)
    private String userName;

    @Column(name = "u_password", nullable = false)
    private String password;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "users_projects",
            joinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "u_id")},
            inverseJoinColumns = {@JoinColumn(name = "pro_id", referencedColumnName = "pro_id")})
    private Set<Projects> projectsSet;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "users_roles",
            joinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "u_id")},
            inverseJoinColumns = {@JoinColumn(name = "role_id", referencedColumnName = "roleId")})
    private Set<Roles> roleSet;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "users_department",
            joinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "u_id")},
            inverseJoinColumns = {@JoinColumn(name = "dep_id", referencedColumnName = "dep_id")})
    private Department department;
}
