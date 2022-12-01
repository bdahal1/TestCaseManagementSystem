package com.tcms.authentication.pojo;


import java.io.Serializable;

public class TokenRefreshRequest  implements Serializable {
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}