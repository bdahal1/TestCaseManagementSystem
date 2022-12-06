package com.tcms.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "project")
@JsonIgnoreProperties("usersSet")
public class Projects {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pro_id", nullable = false)
    private int id;

    @Column(name = "pro_name", nullable = false)
    private String projectName;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "users_projects",
            joinColumns = {@JoinColumn(name = "pro_id", referencedColumnName = "pro_id")},
            inverseJoinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "u_id")})
    private Set<Users> usersSet;
}
