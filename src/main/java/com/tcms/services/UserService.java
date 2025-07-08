package com.tcms.services;

import com.tcms.dto.ProjectDTO;
import com.tcms.dto.UsersInfoDTO;
import com.tcms.helper.pojo.CustomResponseMessage;
import com.tcms.models.Projects;
import com.tcms.models.Roles;
import com.tcms.models.Users;
import com.tcms.repositories.DepartmentRepository;
import com.tcms.repositories.ProjectRepository;
import com.tcms.repositories.RoleRepository;
import com.tcms.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final ProjectRepository projectRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, DepartmentRepository departmentRepository, ProjectRepository projectRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.projectRepository = projectRepository;
        this.passwordEncoder = passwordEncoder;
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

    public ResponseEntity<Object> saveUser(UsersInfoDTO usersInfoDTO) {
        Set<Roles> roleSet = new HashSet<>();
        Set<Projects> projectsSet = getListOfProjects(usersInfoDTO);
        try {
            roleSet.add(roleRepository.findByRoleId(usersInfoDTO.getRoleId()));
            Users user = new Users();
            user.setFirstName(usersInfoDTO.getFirstName());
            user.setLastName(usersInfoDTO.getLastName());
            user.setUserName(usersInfoDTO.getUserName());
            user.setDepartment(departmentRepository.findByDepId(usersInfoDTO.getDepartmentId()));
            user.setRoleSet(roleSet);
            user.setProjectsSet(projectsSet);
            user.setPassword(passwordEncoder.encode(usersInfoDTO.getPassword()));
            saveUserOperation(user);
            return ResponseEntity.status(HttpStatus.OK).body(removePasswordForGivenUser(user));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CustomResponseMessage(new Date(), "Error", e.getCause().getCause().getLocalizedMessage()));
        }
    }

    private Set<Projects> getListOfProjects(UsersInfoDTO usersInfoDTO) {
        Set<Projects> projectsSet = new HashSet<>();
        List<Integer> idList = usersInfoDTO.getProjectsSet()
                .stream()
                .map(ProjectDTO::getId)
                .toList();
        if (!idList.isEmpty()) {
            for (Integer id : idList) {
                projectsSet.add(projectRepository.findById(id));
            }
        }
        return projectsSet;
    }

    public ResponseEntity<Object> editUser(UsersInfoDTO usersInfoDTO, Integer userId) {
        try {
            Set<Projects> projectsSet = getListOfProjects(usersInfoDTO);
            Users user = userRepository.findById(userId);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User Not Found in database.\n");
            }
            user.setFirstName(usersInfoDTO.getFirstName() == null ? user.getFirstName() : usersInfoDTO.getFirstName());
            user.setLastName(usersInfoDTO.getLastName() == null ? user.getLastName() : usersInfoDTO.getLastName());
            user.setUserName(usersInfoDTO.getUserName() == null ? user.getUserName() : usersInfoDTO.getUserName());
            user.setPassword(user.getPassword());
            user.setDepartment(usersInfoDTO.getDepartmentId() == null ? user.getDepartment() : departmentRepository.findByDepId(usersInfoDTO.getDepartmentId()));
            user.setRoleSet(usersInfoDTO.getRoleId() == null ? user.getRoleSet() : new HashSet<>(Arrays.asList(roleRepository.findByRoleId(usersInfoDTO.getRoleId()))));
            user.setProjectsSet(usersInfoDTO.getProjectsSet() == null ? user.getProjectsSet() : new HashSet<>(projectsSet));
            user.setIsActive(usersInfoDTO.getIsActive() == null ? user.getIsActive() : usersInfoDTO.getIsActive());
            saveUserOperation(user);
            return ResponseEntity.status(HttpStatus.OK).body(removePasswordForGivenUser(user));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CustomResponseMessage(new Date(), "Error", e.getCause().getCause().getLocalizedMessage()));
        }
    }

    public void deleteUser(Users user) {
        this.saveUserOperation(user);
    }

    private void saveUserOperation(Users user) {
        userRepository.save(user);
    }
}
