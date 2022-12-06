package com.tcms.repositories;


import com.tcms.models.Department;
import com.tcms.models.Roles;
import com.tcms.models.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<Users, Integer> {

    List<Users> findAll();

    Users findById(int id);

    Users findByUserName(String username);

    void deleteById(int id);

    List<Users> findUsersByRoleSetIn(List<Integer> roleIds);

    Users findUsersByDepartment_DepName(String depName);
}

