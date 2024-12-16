package com.tcms.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class UsersInfoDTO {
    private String firstName;
    private String lastName;
    private String userName;
    private String password;
    private Integer roleId;
    private List<ProjectDTO> projectsSet;
    private Integer departmentId;
    private Boolean isActive;
}
