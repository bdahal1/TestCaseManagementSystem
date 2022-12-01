package com.tcms.authentication.pojo;

import java.io.Serializable;

public record JwtResponse(TokenRefreshResponse data) implements Serializable {

	private static final long serialVersionUID = -8091879091924046844L;


}