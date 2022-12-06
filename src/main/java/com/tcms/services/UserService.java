package com.tcms.services;

import com.tcms.models.Users;
import com.tcms.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {
    final
    UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<Users> removePasswordFromList(List<Users> usersList) {
        List<Users> tempList = new ArrayList<>();
        for (Users users : usersList) {
            users.setPassword("");
            tempList.add(users);
        }
        return tempList;
    }

    public Users removePasswordForGivenUser(Users users) {
        users.setPassword("");
        return users;
    }
}
