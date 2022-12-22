package com.tcms.repositories;


import com.tcms.models.Projects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends PagingAndSortingRepository<Projects, Integer>, JpaSpecificationExecutor<Projects> {

    Page<Projects> findAll(Pageable pageable);

    Projects findById(int id);

    Page<Projects> findByProjectNameContaining(String projectName, Pageable pageable);

    void deleteById(int id);

    Projects save(Projects projects);
}
