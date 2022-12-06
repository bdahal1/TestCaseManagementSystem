package com.tcms.repositories;


import com.tcms.models.Department;
import com.tcms.models.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Integer> {

    List<Department> findAll();
    Department findByDepName(String depName);

}
