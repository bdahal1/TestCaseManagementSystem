package com.tcms.repositories;


import com.tcms.models.Projects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends CrudRepository<Projects, Integer> {

    Page<Projects> findAll(Pageable pageable);

    Projects findById(int id);

    Page<Projects> findByProjectNameContaining(String projectName, Pageable pageable);
}
