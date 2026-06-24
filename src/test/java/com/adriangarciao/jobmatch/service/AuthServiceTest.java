package com.adriangarciao.jobmatch.service;

import com.adriangarciao.jobmatch.dto.AuthResponse;
import com.adriangarciao.jobmatch.dto.LoginRequest;
import com.adriangarciao.jobmatch.dto.RegisterRequest;
import com.adriangarciao.jobmatch.model.Role;
import com.adriangarciao.jobmatch.model.User;
import com.adriangarciao.jobmatch.repository.ResumeRepository;
import com.adriangarciao.jobmatch.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private ResumeRepository resumeRepository;
    @Mock
    private Authentication authentication;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(userRepository, passwordEncoder, jwtService, resumeRepository);
    }

    @Test
    void register_successful() {
        RegisterRequest req = new RegisterRequest("Test User", "test@example.com", "password123");
        when(userRepository.existsByEmail(req.email())).thenReturn(false);
        when(passwordEncoder.encode(req.password())).thenReturn("hashed");
        when(jwtService.generateToken(any(), any(), any())).thenReturn("token");
        AuthResponse response = authService.register(req);
        assertEquals("token", response.token());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_emailConflict() {
        RegisterRequest req = new RegisterRequest("Test User", "test@example.com", "password123");
        when(userRepository.existsByEmail(req.email())).thenReturn(true);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.register(req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void login_successful() {
        LoginRequest req = new LoginRequest("test@example.com", "password123");
        User user = new User();
        user.setEmail(req.email());
        user.setPasswordHash("hashed");
        user.setRole(Role.USER);
        user.setId(1L);
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(req.password(), user.getPasswordHash())).thenReturn(true);
        when(jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name())).thenReturn("token");
        AuthResponse response = authService.login(req);
        assertEquals("token", response.token());
    }

    @Test
    void login_invalidCredentials_email() {
        LoginRequest req = new LoginRequest("test@example.com", "password123");
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.login(req));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void login_invalidCredentials_password() {
        LoginRequest req = new LoginRequest("test@example.com", "password123");
        User user = new User();
        user.setEmail(req.email());
        user.setPasswordHash("hashed");
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(req.password(), user.getPasswordHash())).thenReturn(false);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.login(req));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void ownsResume_returnsTrueIfOwner() {
        when(authentication.getName()).thenReturn("test@example.com");
        when(resumeRepository.findOwnerEmailById(1L)).thenReturn(Optional.of("test@example.com"));
        assertTrue(authService.ownsResume(authentication, 1L));
    }

    @Test
    void ownsResume_returnsFalseIfNotOwner() {
        when(authentication.getName()).thenReturn("test@example.com");
        when(resumeRepository.findOwnerEmailById(1L)).thenReturn(Optional.of("other@example.com"));
        assertFalse(authService.ownsResume(authentication, 1L));
    }

    @Test
    void ownsResume_returnsFalseIfNoAuthOrId() {
        assertFalse(authService.ownsResume(null, 1L));
        assertFalse(authService.ownsResume(authentication, null));
    }

    @Test
    void ownsResume_returnsFalseIfNoOwnerFound() {
        when(authentication.getName()).thenReturn("test@example.com");
        when(resumeRepository.findOwnerEmailById(1L)).thenReturn(Optional.empty());
        assertFalse(authService.ownsResume(authentication, 1L));
    }
}
