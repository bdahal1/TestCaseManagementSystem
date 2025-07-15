package com.tcms.controller;

import com.tcms.dto.UsersInfoDTO;
import com.tcms.helper.pojo.CustomResponseMessage;
import com.tcms.models.Users;
import com.tcms.repositories.UserRepository;
import com.tcms.services.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@RestController()
@CrossOrigin()
@RequestMapping("/users")
public class UserController {
    private final String defaultSize = "1000";
    private final UserRepository userRepository;
    private final UserService userService;

    public UserController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @GetMapping("")
    public ResponseEntity<Object> getUsers(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = defaultSize) int size, @RequestParam(required = false) Boolean status) {
        Pageable paging = PageRequest.of(page, size);
        Page<Users> usersList;
        if (status == null) {
            usersList = userService.removePasswordFromList(userRepository.findAll(paging), paging);
        } else {
            usersList = userService.removePasswordFromList(userRepository.findUsersByIsActive(status, paging), paging);
        }
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
    public ResponseEntity<Object> getUsersByRoleIds(@PathVariable String roleIds, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = defaultSize) int size) {
        Pageable paging = PageRequest.of(page, size);
        List<Integer> roles = new ArrayList<>();
        for (String id : roleIds.split(",")) {
            int role = Integer.parseInt(id);
            roles.add(role);
        }
        Page<Users> users = userRepository.findUsersByRoleSetIn(roles, paging);
        if (users == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record not found.\n");
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(userService.getUserListResponse(users, paging));
        }
    }

    @PostMapping("")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> saveUser(@RequestBody UsersInfoDTO usersInfoDTO) {
        if (usersInfoDTO == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User Info Not Found inside body.\n");
        }
        return this.userService.saveUser(usersInfoDTO);
    }

    @PutMapping("/{userId}")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> editUser(@RequestBody UsersInfoDTO usersInfoDTO, @PathVariable int userId) {
        if (usersInfoDTO == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User Info Not Found inside body.\n");
        }
        return this.userService.editUser(usersInfoDTO, userId);
    }

    @GetMapping("/{userId}/projects")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> getUserProjects(@PathVariable int userId) {
        if (userId == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User Id Not Found inside body.\n");
        }
        return ResponseEntity.status(HttpStatus.OK).body(this.userRepository.findById(userId).getProjectsSet());
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
        user.setIsActive(false);
        this.userService.deleteUser(user);
        return ResponseEntity.status(HttpStatus.OK).body(new CustomResponseMessage(new Date(), "Success", "User Soft Deleted Successfully!"));
    }
}
