package com.adriangarciao.jobmatch.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private JwtService jwtService;
    private final String secret = Base64.getEncoder().encodeToString("this_is_a_very_secret_key_for_jwt_123456".getBytes());

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(secret, 60L);
    }

    @Test
    void generateToken_and_parseToken_success() {
        String token = jwtService.generateToken(42L, "user@example.com", "USER");
        assertNotNull(token);
        Jws<Claims> parsed = jwtService.parse(token);
        assertEquals("user@example.com", parsed.getBody().getSubject());
        assertEquals(42L, ((Number)parsed.getBody().get("uid")).longValue());
        assertEquals("USER", parsed.getBody().get("role"));
        assertNotNull(parsed.getBody().getIssuedAt());
        assertNotNull(parsed.getBody().getExpiration());
    }

    @Test
    void generateToken_withShortSecret_throws() {
        String shortSecret = Base64.getEncoder().encodeToString("short".getBytes());
        Exception ex = assertThrows(IllegalStateException.class, () -> new JwtService(shortSecret, 60L));
        assertTrue(ex.getMessage().contains("at least 32 bytes"));
    }

    @Test
    void parse_invalidToken_throws() {
        String invalidToken = "not.a.jwt.token";
        assertThrows(Exception.class, () -> jwtService.parse(invalidToken));
    }
}
