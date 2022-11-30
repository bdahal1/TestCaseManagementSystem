package com.tcms.repositories;


import com.tcms.models.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<Users, Integer> {

    List<Users> findAll();
    Users findById(int id);
    Users findByUserName(String username);
    Users findByUserNameAndPassword(String userName, String password);
    void deleteById(int id);
}
