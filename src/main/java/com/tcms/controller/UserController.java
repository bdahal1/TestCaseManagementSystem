package com.tcms.controller;

import com.tcms.models.Users;
import com.tcms.repositories.UserRepository;
import com.tcms.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController()
@CrossOrigin()
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    public UserController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }


    @GetMapping("")
    public ResponseEntity<Object> getUsers() {
        List<Users> usersList = userService.removePasswordFromList();
        if (usersList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record not found.\n");
        }
        return ResponseEntity.status(HttpStatus.OK).body(userService.removePasswordFromList());
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
}
