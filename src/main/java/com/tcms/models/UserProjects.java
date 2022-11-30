package com.tcms.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.NonNull;

@Entity
@Getter
@Setter
@Table(name="user_projects")
public class UserProjects {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "up_id")
    @NonNull
    private int id;

    @Column(name = "user_id")
    @NonNull
    private int userId;

    @Column(name = "p_id")
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
