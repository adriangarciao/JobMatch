package com.adriangarciao.jobmatch.controller;

import com.adriangarciao.jobmatch.dto.LoginRequest;
import com.adriangarciao.jobmatch.dto.RegisterRequest;
import com.adriangarciao.jobmatch.model.Role;
import com.adriangarciao.jobmatch.model.User;
import com.adriangarciao.jobmatch.service.JwtService;
import com.adriangarciao.jobmatch.service.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
 
import com.adriangarciao.jobmatch.repository.UserRepository;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@WebMvcTest(AuthController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@Import(AuthControllerTest.TestConfig.class)
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private RefreshTokenService refreshTokenService;
    @Autowired
    private ObjectMapper objectMapper;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed");
        user.setRole(Role.USER);
    }

    @Test
    void register_success() throws Exception {
        RegisterRequest req = new RegisterRequest("Test User", "test@example.com", "password123");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(jwtService.generateToken(any(), any(), any())).thenReturn("token");
        when(refreshTokenService.create(any(User.class))).thenReturn(Mockito.mock(com.adriangarciao.jobmatch.model.RefreshToken.class));
        when(refreshTokenService.cookieMaxAgeSeconds()).thenReturn(3600);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token"));
    }

    @Test
    void register_conflict() throws Exception {
        RegisterRequest req = new RegisterRequest("Test User", "test@example.com", "password123");
        when(userRepository.existsByEmail(anyString())).thenReturn(true);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.token").value("Email already in use"));
    }

    @Test
    void login_success() throws Exception {
        LoginRequest req = new LoginRequest("test@example.com", "password123");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtService.generateToken(any(), any(), any())).thenReturn("token");
        when(refreshTokenService.create(any(User.class))).thenReturn(Mockito.mock(com.adriangarciao.jobmatch.model.RefreshToken.class));
        when(refreshTokenService.cookieMaxAgeSeconds()).thenReturn(3600);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token"));
    }

    @Test
    void login_invalidCredentials() throws Exception {
        LoginRequest req = new LoginRequest("test@example.com", "password123");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public UserRepository userRepository() { return Mockito.mock(UserRepository.class); }

        @Bean
        public PasswordEncoder passwordEncoder() { return Mockito.mock(PasswordEncoder.class); }

        @Bean
        public JwtService jwtService() { return Mockito.mock(JwtService.class); }

        @Bean
        public RefreshTokenService refreshTokenService() { return Mockito.mock(RefreshTokenService.class); }
    }

}

