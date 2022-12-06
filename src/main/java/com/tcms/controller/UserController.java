package com.tcms.controller;

import com.tcms.models.Users;
import com.tcms.repositories.DepartmentRepository;
import com.tcms.repositories.RoleRepository;
import com.tcms.repositories.UserRepository;
import com.tcms.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@RestController()
@CrossOrigin()
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    private final RoleRepository roleRepository;

    private final DepartmentRepository departmentRepository;

    public UserController(UserRepository userRepository, UserService userService, RoleRepository roleRepository, DepartmentRepository departmentRepository) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
    }


    @GetMapping("")
    public ResponseEntity<Object> getUsers() {
        List<Users> usersList = userService.removePasswordFromList(userRepository.findAll());
        if (usersList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record not found.\n");
        }
        return ResponseEntity.status(HttpStatus.OK).body(userService.removePasswordFromList(usersList));
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
    public ResponseEntity<Object> getUsersByRoleIds(@PathVariable String roleIds) {
        List<Integer> roles= new ArrayList<>();
        for(String id: roleIds.split(",")){
            int role= Integer.parseInt(id);
            roles.add(role);
        }
        List<Users> users = userRepository.findUsersByRoleSetIn(roles);
        if (users == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record not found.\n");
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(userService.removePasswordFromList(users));
        }
    }

    @PostMapping("")
    public ResponseEntity<Object> saveUser(@RequestBody Users users) {
        if (users==null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User Info Not Found inside body.\n");
        }
        return ResponseEntity.status(HttpStatus.OK).body(userService.removePasswordForGivenUser(userRepository.save(users)));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Object> deleteUser(@PathVariable String userId) {
        if (userId==null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("userId not found for delete operation.\n");
        }
        Users user=userRepository.findById(Integer.parseInt(userId));
        if(user==null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.\n");
        }
        userRepository.deleteById(Integer.parseInt(userId));
        return ResponseEntity.status(HttpStatus.OK).body("User Deleted!!");
    }
}
