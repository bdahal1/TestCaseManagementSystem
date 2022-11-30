package com.tcms.models;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.lang.NonNull;

import java.util.List;

@Entity
@Getter
@Setter
@Table(name="project")
public class Projects {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "pro_id")
    @NonNull
    private int id;

    @Column(name = "pro_name")
    @NonNull
    private String projectName;

    @ManyToMany(cascade = CascadeType.ALL, mappedBy = "projectId", fetch = FetchType.EAGER)
    private List<UserProjects> userProjectsList;

    @Override
    public String toString() {
        return "Projects{" +
                "id=" + id +
                ", projectName='" + projectName + '\'' +
                ", userProjectsList=" + userProjectsList +
                '}';
    }
}
