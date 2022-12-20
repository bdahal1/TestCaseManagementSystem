package com.tcms.services;

import com.tcms.models.Users;
import com.tcms.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {
    final
    UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Page<Users> removePasswordFromList(Page<Users> usersList, Pageable pageable) {
        List<Users> tempList = new ArrayList<>();
        for (Users users : usersList) {
            users.setPassword("");
            tempList.add(users);
        }
        return new PageImpl<>(tempList, pageable, pageable.getPageSize());
    }

    public Users removePasswordForGivenUser(Users users) {
        users.setPassword("");
        return users;
    }

    public Map<String, Object> getUserListResponse(Page<Users> usersList, Pageable pageable) {
        Map<String, Object> response = new HashMap<>();
        response.put("users", removePasswordFromList(usersList, pageable).getContent());
        response.put("currentPage", usersList.getNumber());
        response.put("totalItems", usersList.getTotalElements());
        response.put("totalPages", usersList.getTotalPages());
        return response;
    }
}
