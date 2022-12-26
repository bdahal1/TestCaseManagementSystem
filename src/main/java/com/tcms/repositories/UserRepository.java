package com.tcms.repositories;


import com.tcms.models.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends PagingAndSortingRepository<Users, Integer>, JpaSpecificationExecutor<Users> {

    Page<Users> findAll(Pageable pageable);

    Users findById(int id);

    Users findByUserName(String username);

    Page<Users> findUsersByIsActive(boolean status, Pageable pageable);

    void deleteById(int id);

    Page<Users> findUsersByRoleSetIn(List<Integer> roleIds, Pageable pageable);

    Users findUsersByDepartment_DepName(String depName);

    Users save(Users users);
}

