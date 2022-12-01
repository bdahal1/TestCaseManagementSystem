package com.tcms.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.NonNull;

import java.util.List;

@Entity
@Getter
@Setter
@Table(name="department")
@SequenceGenerator(name = "")
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dep_id")
    @NonNull
    private int depId;

    @Column(name = "dep_name")
    @NonNull
    private String depName;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "users_department",
            joinColumns = {@JoinColumn(name = "dep_id", referencedColumnName = "dep_id")},
            inverseJoinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "user_id")})
    private List<UserDepartment> userDepartmentList;

}
