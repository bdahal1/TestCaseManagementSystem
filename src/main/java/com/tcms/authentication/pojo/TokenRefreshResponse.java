package com.tcms.authentication.pojo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TokenRefreshResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private List<String> rolesList;
    private long userId = 0;
    private String fullName;

    public TokenRefreshResponse(String accessToken, String refreshToken, long userId, List<String> rolesList, String fullName) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userId = userId;
        this.rolesList = rolesList;
        this.fullName = fullName;
    }

}