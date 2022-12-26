package com.tcms.models.intermediate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Getter
@Setter
@Table(name = "users_projects")
@AllArgsConstructor
@NoArgsConstructor
public class UserProjects implements Serializable {
    @Id
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "pro_id", nullable = false)
    private Integer projectId;
}
