package com.tcms.authentication.repository;

import com.tcms.authentication.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {
    Optional<RefreshToken> findByToken(String token);
    RefreshToken findByUserName(String userName);
    int deleteByUserName(String userName);
}