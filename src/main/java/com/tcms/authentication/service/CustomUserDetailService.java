package com.tcms.authentication.service;

import com.tcms.models.Users;
import com.tcms.repositories.RoleRepository;
import com.tcms.repositories.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    public CustomUserDetailService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Users user = userRepository.findByUserName(username);
        if(user.getRoleSet().isEmpty()){
            return new org.springframework.security.core.userdetails.User(user.getUserName(), user.getPassword(), new HashSet<>());
        }
        Set<GrantedAuthority> grantedAuthorities = user.getRoleSet().stream()
                .map(userRoles ->
                        new SimpleGrantedAuthority(roleRepository.findByRoleId(userRoles.getRoleId()).getRoleName())
                )
                .collect(Collectors.toSet());
        return new org.springframework.security.core.userdetails.User(user.getUserName(), user.getPassword(), grantedAuthorities);
    }
}
