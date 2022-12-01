package com.tcms.authentication.pojo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class JwtRequest implements Serializable {

	private static final long serialVersionUID = 5926468583005150707L;
	
	private String userName;
	private String password;

	public JwtRequest(String userName, String password) {
		this.setUserName(userName);
		this.setPassword(password);
	}

	@Override
	public String toString() {
		return "JwtRequest{" +
				"userName='" + userName + '\'' +
				", password='" + password + '\'' +
				'}';
	}
}