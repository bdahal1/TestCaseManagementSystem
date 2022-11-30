package com.tcms.repositories;


import com.tcms.models.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Integer> {

    List<TestCase> findAll();
    TestCase findById(int id);
    void deleteById(int tcId);
}
