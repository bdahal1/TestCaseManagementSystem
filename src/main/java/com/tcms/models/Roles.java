package com.tcms.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.NonNull;

import java.util.List;

@Entity
@Getter
@Setter
@Table(name="role")
public class Roles {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column
    @NonNull
    private int roleId;

    @Column
    @NonNull
    private String roleName;

    @ManyToMany(cascade = CascadeType.ALL, mappedBy = "roleId", fetch = FetchType.EAGER)
    private List<UserRoles> userRolesList;

    @Override
    public String toString() {
        return "Roles{" +
                "roleId=" + roleId +
                ", roleName='" + roleName + '\'' +
                ", userRolesList=" + userRolesList +
                '}';
    }
}
