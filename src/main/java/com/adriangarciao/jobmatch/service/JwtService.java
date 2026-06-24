package com.adriangarciao.jobmatch.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final Key key;
    private final long expMinutes;

    public JwtService(
            @Value("${app.jwt.secret}") String base64Secret,
            @Value("${app.jwt.expMinutes:60}") long expMinutes
    ) {
        // Expect Base64; decode to bytes (must be >= 32 bytes for HS256)
        byte[] secretBytes = Base64.getDecoder().decode(base64Secret);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT secret must be a Base64 string of at least 32 bytes (256 bits) when decoded."
            );
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expMinutes = expMinutes;
    }

    public String generateToken(Long userId, String email, String role) {
        log.debug("Generating JWT for userId: {}, email: {}", userId, email);
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(email)
                .claim("uid", userId)
                .claim("role", role)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(Duration.ofMinutes(expMinutes))))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> parse(String jwt) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(jwt);
    }
}
