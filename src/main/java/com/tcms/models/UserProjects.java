package com.tcms.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.lang.NonNull;

import java.io.Serializable;

@Entity
@Getter
@Setter
@Table(name="users_projects")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class UserProjects implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "up_id")
    private int id;

    @Column(name = "user_id")
    @NonNull
    private int userId;

    @Column(name = "pro_id")
    @NonNull
    private int projectId;

    @Override
    public String toString() {
        return "UserProjects{" +
                "id=" + id +
                ", userId=" + userId +
                ", projectId=" + projectId +
                '}';
    }
}
