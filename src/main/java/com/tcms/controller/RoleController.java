package com.tcms.controller;

import com.tcms.helper.pojo.CustomResponseMessage;
import com.tcms.models.Roles;
import com.tcms.repositories.RoleRepository;
import com.tcms.services.RoleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController()
@CrossOrigin()
@RequestMapping("/roles")
public class RoleController {
    private final RoleRepository roleRepository;
    private final RoleService roleService;

    public RoleController(RoleRepository roleRepository, RoleService roleService) {
        this.roleRepository = roleRepository;
        this.roleService = roleService;
    }

    @GetMapping("")
    public ResponseEntity<Object> getRoles(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Pageable paging = PageRequest.of(page, size);
        Page<Roles> rolesList = roleRepository.findAll(paging);
        if (rolesList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record not found.\n");
        }
        return ResponseEntity.status(HttpStatus.OK).body(roleService.getRoleListResponse(rolesList));
    }

    @GetMapping(path = "/name/{roleName}")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> getRoleByRoleName(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, @PathVariable String roleName) {
        Pageable paging = PageRequest.of(page, size);
        Page<Roles> rolesList = roleRepository.findByRoleNameContaining(roleName, paging);
        if (rolesList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record not found.\n");
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(roleService.getRoleListResponse(rolesList));
        }
    }

    @GetMapping(path = "/id/{id}")
    public ResponseEntity<Object> getRoleById(@PathVariable int id) {
        Roles roles = roleRepository.findByRoleId(id);
        if (roles == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record not found.\n");
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(roles);
        }
    }

    @PostMapping("")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> saveRole(@RequestBody Roles roles) {
        if (roles == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Role Info Not Found inside body.\n");
        }
        try {
            roleRepository.save(roles);
            return ResponseEntity.status(HttpStatus.OK).body(roles);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CustomResponseMessage(new Date(), "Error", e.getCause().getCause().getLocalizedMessage()));
        }
    }

    @PutMapping("/{roleId}")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> editRole(@RequestBody Roles roles, @PathVariable int roleId) {
        if (roles == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Role Info Not Found inside body.\n");
        }
        try {
            Roles role = roleRepository.findByRoleId(roleId);
            if (role == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Role Not Found in database.\n");
            }
            role.setRoleName(roles.getRoleName() == null ? role.getRoleName() : roles.getRoleName());
            roleRepository.save(role);
            return ResponseEntity.status(HttpStatus.OK).body(role);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CustomResponseMessage(new Date(), "Error", e.getCause().getCause().getLocalizedMessage()));
        }
    }

    @DeleteMapping("/{roleId}")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> deleteRole(@PathVariable String roleId) {
        if (roleId == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("roleId not found for delete operation.\n");
        }
        Roles role = roleRepository.findByRoleId(Integer.parseInt(roleId));
        if (role == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new CustomResponseMessage(new Date(), "Error", "Provided role not found for Delete operation!"));
        }
        roleRepository.deleteById(Integer.parseInt(roleId));
        return ResponseEntity.status(HttpStatus.OK).body(new CustomResponseMessage(new Date(), "Success", "Role Deleted Successfully!"));
    }
}
