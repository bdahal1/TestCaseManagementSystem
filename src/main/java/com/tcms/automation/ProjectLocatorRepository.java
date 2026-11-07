package com.tcms.automation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectLocatorRepository extends JpaRepository<ProjectLocator, Integer> {
    List<ProjectLocator> findByProjectIdOrderByLocatorNameAsc(Integer projectId);
    Optional<ProjectLocator> findByProjectIdAndLocatorName(Integer projectId, String locatorName);
    boolean existsByProjectIdAndLocatorName(Integer projectId, String locatorName);
}
