package com.tcms.repositories;


import com.tcms.models.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends CrudRepository<Department, Integer> {

    Page<Department> findAll(Pageable pageable);

    Page<Department> findByDepNameIsContaining(String depName, Pageable pageable);

    Department findByDepId(int depId);

}
