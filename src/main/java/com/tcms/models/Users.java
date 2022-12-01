package com.tcms.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name="users")
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "u_id")
    @NonNull
    private int id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "u_name")
    private String userName;

    @Column(name = "u_password")
    private String password;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(name = "users_projects",
            joinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "u_id")},
            inverseJoinColumns = {@JoinColumn(name = "pro_id", referencedColumnName = "pro_id")})
    private Set<UserProjects> userProjectsList;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(name = "users_roles",
            joinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "u_id")},
            inverseJoinColumns = {@JoinColumn(name = "role_id", referencedColumnName = "role_id")})
    private Set<UserRoles> userRolesList;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(name = "users_department",
            joinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "u_id")},
            inverseJoinColumns = {@JoinColumn(name = "dep_id", referencedColumnName = "dep_id")})
    private Set<UserDepartment> userDepartmentList;

    @Override
    public String toString() {
        return "Users{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", userName='" + userName + '\'' +
                ", userProjectsList=" + userProjectsList +
                ", userRolesList=" + userRolesList +
                '}';
    }
}
