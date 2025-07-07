package com.tcms.authentication.controller;

import com.tcms.authentication.config.JwtTokenUtil;
import com.tcms.authentication.pojo.JwtRequest;
import com.tcms.authentication.pojo.JwtResponse;
import com.tcms.authentication.model.RefreshToken;
import com.tcms.authentication.pojo.TokenRefreshRequest;
import com.tcms.authentication.pojo.TokenRefreshResponse;
import com.tcms.authentication.service.CustomUserDetailService;
import com.tcms.authentication.service.RefreshTokenService;
import com.tcms.authentication.service.TokenRefreshException;
import com.tcms.helper.pojo.CustomResponseMessage;
import com.tcms.models.Roles;
import com.tcms.models.Users;
import com.tcms.repositories.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Objects;
import java.util.stream.Collectors;


@RestController
@CrossOrigin
@Lazy
public class JwtAuthenticationController {

    private final AuthenticationManager authenticationManager;

    private final JwtTokenUtil jwtTokenUtil;

    private final CustomUserDetailService customUserDetailService;
    final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    public JwtAuthenticationController(AuthenticationManager authenticationManager, JwtTokenUtil jwtTokenUtil, CustomUserDetailService customUserDetailService, RefreshTokenService refreshTokenService, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtil = jwtTokenUtil;
        this.customUserDetailService = customUserDetailService;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
    }

    @GetMapping("/validate")
    public ResponseEntity<String> validateToken() {
        return ResponseEntity.ok("Token is valid");
    }

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public ResponseEntity<?> createAuthenticationToken(@RequestBody JwtRequest authenticationRequest) {
        try {
            authenticate(authenticationRequest.getUserName(), authenticationRequest.getPassword());
            final UserDetails userDetails = customUserDetailService.loadUserByUsername(authenticationRequest.getUserName());
            if (userDetails.getAuthorities().isEmpty())
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new CustomResponseMessage(new Date(), "Login Issue", "Authorities/Roles not found for the user!"));
            final String accessToken = jwtTokenUtil.generateToken(userDetails);
            final String refToken = refreshTokenService.createRefreshToken(userDetails).getToken();
            Users user = userRepository.findByUserName(authenticationRequest.getUserName());
            TokenRefreshResponse responseBody = new TokenRefreshResponse(accessToken, refToken, user.getId(), user.getRoleSet().stream().map(Roles::getRoleName).collect(Collectors.toList()), user.getFirstName() + " " + user.getLastName());
            if (accessToken == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record not found.\n");
            } else {
                return ResponseEntity.status(HttpStatus.OK).body(new JwtResponse(responseBody));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new CustomResponseMessage(new Date(), "Login Issue", e.getMessage()));
        }
    }

    @PostMapping("/refreshToken")
    public ResponseEntity<?> refreshToken(@RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();
        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUserName)
                .map(user -> {
                    String token = jwtTokenUtil.generateToken(customUserDetailService.loadUserByUsername(user));
                    Users loggedInUser = userRepository.findByUserName(user);
                    return ResponseEntity.ok(new JwtResponse(new TokenRefreshResponse(token, requestRefreshToken, loggedInUser.getId(), loggedInUser.getRoleSet().stream().map(Roles::getRoleName).collect(Collectors.toList()), loggedInUser.getFirstName() + " " + loggedInUser.getLastName())));
                })
                .orElseThrow(() -> new TokenRefreshException(requestRefreshToken,
                        "Refresh token is not in database!"));
    }

    private void authenticate(String username, String password) throws Exception {
        Objects.requireNonNull(username);
        Objects.requireNonNull(password);
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (DisabledException e) {
            throw new Exception("USER_DISABLED", e);
        } catch (BadCredentialsException e) {
            throw new Exception("INVALID_CREDENTIALS", e);
        }
    }
}
