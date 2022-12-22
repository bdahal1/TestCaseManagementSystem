package com.tcms.repositories;


import com.tcms.models.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends PagingAndSortingRepository<Department, Integer>, JpaSpecificationExecutor<Department> {

    Page<Department> findAll(Pageable pageable);

    Page<Department> findByDepNameIsContaining(String depName, Pageable pageable);

    Department findByDepId(int depId);

    void deleteById(int id);

    Department save(Department department);

}
