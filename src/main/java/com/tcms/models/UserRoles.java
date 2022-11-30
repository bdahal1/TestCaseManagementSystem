package com.tcms.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.NonNull;

@Entity
@Getter
@Setter
@Table(name="user_roles")
public class UserRoles {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @NonNull
    @Column(name = "ur_id")
    private int id;

    @Column(name = "u_id")
    @NonNull
    private int userId;

    @Column(name = "role_id")
    @NonNull
    private int roleId;

    @Override
    public String toString() {
        return "UserRoles{" +
                "id=" + id +
                ", userId=" + userId +
                ", roleId=" + roleId +
                '}';
    }
}
