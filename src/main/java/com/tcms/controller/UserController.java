package com.tcms.controller;

import com.tcms.helper.pojo.CustomResponseMessage;
import com.tcms.models.Users;
import com.tcms.repositories.DepartmentRepository;
import com.tcms.repositories.RoleRepository;
import com.tcms.repositories.UserRepository;
import com.tcms.services.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;


@RestController()
@CrossOrigin()
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, UserService userService, RoleRepository roleRepository, DepartmentRepository departmentRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }


    @GetMapping("")
    public ResponseEntity<Object> getUsers(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Pageable paging = PageRequest.of(page, size);
        Page<Users> usersList = userService.removePasswordFromList(userRepository.findAll(paging), paging);
        if (usersList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record not found.\n");
        }
        return ResponseEntity.status(HttpStatus.OK).body(userService.getUserListResponse(usersList, paging));
    }

    @GetMapping(path = "/username/{username}")
    public ResponseEntity<Object> getUsersByUName(@PathVariable String username) {
        Users users = userRepository.findByUserName(username);
        if (users == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record not found.\n");
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(userService.removePasswordForGivenUser(users));
        }
    }

    @GetMapping(path = "/id/{id}")
    public ResponseEntity<Object> getUsersById(@PathVariable int id) {
        Users users = userRepository.findById(id);
        if (users == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record not found.\n");
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(userService.removePasswordForGivenUser(users));
        }
    }

    @GetMapping(path = "/department/{depName}")
    public ResponseEntity<Object> getUsersByDepartmentName(@PathVariable String depName) {
        Users users = userRepository.findUsersByDepartment_DepName(depName);
        if (users == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record not found.\n");
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(userService.removePasswordForGivenUser(users));
        }
    }

    @GetMapping(path = "/role/{roleIds}")
    public ResponseEntity<Object> getUsersByRoleIds(@PathVariable String roleIds, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        List<Integer> roles = new ArrayList<>();
        for (String id : roleIds.split(",")) {
            int role = Integer.parseInt(id);
            roles.add(role);
        }
        Pageable paging = PageRequest.of(page, size);
        Page<Users> users = userRepository.findUsersByRoleSetIn(roles, paging);
        if (users == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record not found.\n");
        } else {

            return ResponseEntity.status(HttpStatus.OK).body(userService.getUserListResponse(users, paging));
        }
    }

    @PostMapping("")
    public ResponseEntity<Object> saveUser(@RequestBody Users users) {
        if (users == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User Info Not Found inside body.\n");
        }
        try {
            Users user = users;
            user.setPassword(passwordEncoder.encode(users.getPassword()));
            user = userRepository.save(users);
            return ResponseEntity.status(HttpStatus.OK).body(userService.removePasswordForGivenUser(user));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CustomResponseMessage(new Date(), "Error", e.getCause().getCause().getLocalizedMessage()));
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Object> deleteUser(@PathVariable String userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("userId not found for delete operation.\n");
        }
        Users user = userRepository.findById(Integer.parseInt(userId));
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new CustomResponseMessage(new Date(), "Error", "Provided user not found for Delete operation!"));
        }
        userRepository.deleteById(Integer.parseInt(userId));
        return ResponseEntity.status(HttpStatus.OK).body(new CustomResponseMessage(new Date(), "Success", "User Deleted Successfully!"));
    }
}
