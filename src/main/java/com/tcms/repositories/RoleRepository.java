package com.tcms.repositories;


import com.tcms.models.Roles;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends PagingAndSortingRepository<Roles, Integer>, JpaSpecificationExecutor<Roles> {

    Page<Roles> findAll(Pageable pageable);

    Roles findByRoleId(int roleId);

    Page<Roles> findByRoleNameContaining(String roleName, Pageable pageable);

    void deleteById(int id);

    Roles save(Roles roles);
}
