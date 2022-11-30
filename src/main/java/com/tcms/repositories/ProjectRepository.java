package com.tcms.repositories;


import com.tcms.models.Projects;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Projects, Integer> {

    List<Projects> findAll();
}
