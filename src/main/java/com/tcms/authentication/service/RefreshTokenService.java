package com.tcms.authentication.service;

import com.tcms.authentication.model.RefreshToken;
import com.tcms.authentication.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Value("${jwt.refreshTokenExpiry}")
    private long refreshTokenDurationMs;


    final
    RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }


    public RefreshToken createRefreshToken(UserDetails userDetails) {
        RefreshToken token = null, refreshToken;
        try {
            token = refreshTokenRepository.findByUserName(userDetails.getUsername());
        } catch (Exception e) {

        }
        if (token != null) {
            try {
                refreshToken = verifyExpiration(token);
            } catch (TokenRefreshException e) {
                token.setExpiryDate(Instant.now().plusMillis(this.refreshTokenDurationMs));
                token.setToken(UUID.randomUUID().toString());
                refreshToken = token;
            }
        } else {
            refreshToken = new RefreshToken();
            refreshToken.setUserName(userDetails.getUsername());
            refreshToken.setExpiryDate(Instant.now().plusMillis(this.refreshTokenDurationMs));
            refreshToken.setToken(UUID.randomUUID().toString());
        }
        refreshToken = refreshTokenRepository.save(refreshToken);
        return refreshToken;
    }

    public RefreshToken verifyExpiration(RefreshToken token) throws TokenRefreshException {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(), "Refresh token was expired. Please make a new signin request");
        }

        return token;
    }

    @Transactional
    public int deleteByUserName(String username) {
        return refreshTokenRepository.deleteByUserName(username);
    }
}