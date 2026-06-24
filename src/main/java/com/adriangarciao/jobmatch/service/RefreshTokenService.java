package com.adriangarciao.jobmatch.service;

import com.adriangarciao.jobmatch.model.RefreshToken;
import com.adriangarciao.jobmatch.model.User;
import com.adriangarciao.jobmatch.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository repo;

    // how many days a refresh token stays valid
    @Value("${app.refresh.ttlDays:7}")
    private long ttlDays;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repo) {
        this.repo = repo;
    }

    // -----------------------------
    // Helpers
    // -----------------------------
    private String generateToken() {
        byte[] bytes = new byte[64]; // 512-bit random token
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public int cookieMaxAgeSeconds() { return (int) Duration.ofDays(ttlDays).getSeconds(); }

    // -----------------------------
    // API
    // -----------------------------

    /** Create a new refresh token for a user */
    @Transactional
    public RefreshToken create(User user) {
        log.info("Creating refresh token for user id: {}", user.getId());
        String token = generateToken();
        Instant exp = Instant.now().plus(Duration.ofDays(ttlDays));

        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setToken(token);
        rt.setExpiresAt(exp);
        rt.setRevoked(false);

        return repo.save(rt);
    }

    /** Find a valid (not revoked, not expired) token by its string */
    public Optional<RefreshToken> findValid(String token) {
        return repo.findByToken(token)
                .filter(rt -> !rt.isRevoked() && !rt.isExpired());
    }

    /** Rotate a refresh token: revoke old one and issue a new one */
    @Transactional
    public RefreshToken rotate(String token) {
        RefreshToken current = repo.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("invalid refresh token"));

        if (current.isRevoked() || current.isExpired()) {
            throw new IllegalArgumentException("expired/revoked refresh token");
        }

        current.setRevoked(true);
        repo.save(current);

        return create(current.getUser());
    }

    /** Revoke all active tokens for a user (useful on logout) */
    @Transactional
    public void revokeUserTokens(User user) {
        // uses the @Modifying query in RefreshTokenRepository
        repo.revokeAllForUser(user.getId());
    }

    /** Revoke a single token instance */
    @Transactional
    public void revoke(RefreshToken rt) {
        if (!rt.isRevoked()) {
            rt.setRevoked(true);
            repo.save(rt);
        }
    }
}
