package com.tcms.repositories;


import com.tcms.models.Projects;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends CrudRepository<Projects, Integer> {

    List<Projects> findAll();
}
