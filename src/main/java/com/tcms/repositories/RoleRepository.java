package com.tcms.repositories;


import com.tcms.models.Roles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleRepository extends JpaRepository<Roles, Integer> {

    List<Roles> findAll();
    Roles findByRoleId(int roleId);
}
