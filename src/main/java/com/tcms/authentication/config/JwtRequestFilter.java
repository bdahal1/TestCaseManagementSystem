package com.tcms.authentication.config;

import tools.jackson.databind.ObjectMapper;
import com.tcms.authentication.service.CustomUserDetailService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {
    private final CustomUserDetailService customUserDetailService;

    private final JwtTokenUtil jwtTokenUtil;

    public JwtRequestFilter(JwtTokenUtil jwtTokenUtil, CustomUserDetailService customUserDetailService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.customUserDetailService = customUserDetailService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        final String requestTokenHeader = request.getHeader("Authorization");
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String jwtToken = requestTokenHeader.substring(7);
                String username = jwtTokenUtil.getUsernameFromToken(jwtToken);
                UserDetails user = customUserDetailService.loadUserByUsername(username);
                if (!user.isEnabled() || !user.isAccountNonLocked() || !user.isAccountNonExpired()
                        || !user.isCredentialsNonExpired() || !jwtTokenUtil.validateToken(jwtToken, user)) {
                    throw new org.springframework.security.authentication.BadCredentialsException("Invalid session");
                }
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (io.jsonwebtoken.JwtException | IllegalArgumentException
                     | org.springframework.security.core.AuthenticationException e) {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                tools.jackson.databind.json.JsonMapper.builder().build().writeValue(response.getWriter(), Map.of("message", "Session is invalid or expired"));
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
