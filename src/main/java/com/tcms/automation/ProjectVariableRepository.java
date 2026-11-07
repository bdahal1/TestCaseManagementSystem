package com.tcms.automation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectVariableRepository extends JpaRepository<ProjectVariable, Integer> {
    List<ProjectVariable> findByProjectIdOrderByVariableKeyAsc(Integer projectId);
    Optional<ProjectVariable> findByProjectIdAndVariableKey(Integer projectId, String variableKey);
}
