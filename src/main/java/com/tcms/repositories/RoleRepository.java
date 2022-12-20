package com.tcms.repositories;


import com.tcms.models.Roles;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends CrudRepository<Roles, Integer> {

    Page<Roles> findAll(Pageable pageable);

    Roles findByRoleId(int roleId);

    Page<Roles> findByRoleNameContaining(String roleName, Pageable pageable);

    void deleteById(int id);
}
