package com.tcms.models.intermediate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Getter
@Setter
@Table(name = "users_department")
@AllArgsConstructor
@NoArgsConstructor
public class UserDepartment {
    @Id
    @Column(name = "user_id", nullable = false)
    private int userId;

    @Column(name = "dep_id", nullable = false)
    private int depId;
}
