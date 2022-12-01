package com.tcms.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.lang.NonNull;

@Entity
@Getter
@Setter
@Table(name="users_department")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class UserDepartment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ud_id")
    private int id;

    @Column(name = "user_id")
    @NonNull
    private int userId;

    @Column(name = "dep_id")
    @NonNull
    private int depId;
}
