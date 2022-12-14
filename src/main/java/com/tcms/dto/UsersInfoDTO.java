package com.tcms.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsersInfoDTO {
    private String firstName;
    private String lastName;
    private String userName;
    private String password;
    private Integer roleId;
    private Integer projectId;
    private Integer departmentId;
    private Boolean isActive;
}
