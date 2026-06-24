package com.adriangarciao.jobmatch.service;

import com.adriangarciao.jobmatch.dto.AuthResponse;
import com.adriangarciao.jobmatch.dto.LoginRequest;
import com.adriangarciao.jobmatch.dto.RegisterRequest;
import com.adriangarciao.jobmatch.model.Role;
import com.adriangarciao.jobmatch.model.User;
import com.adriangarciao.jobmatch.repository.ResumeRepository;
import com.adriangarciao.jobmatch.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ResumeRepository resumeRepo;

    public AuthService(UserRepository userRepo
            , PasswordEncoder passwordEncoder
            , JwtService jwtService,
                       ResumeRepository  resumeRepo) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.resumeRepo = resumeRepo;
    }

    public boolean ownsResume(Authentication auth, Long resumeId) {
        if (auth == null || resumeId == null) return false;
        String email = auth.getName(); // principal = email
        return resumeRepo.findOwnerEmailById(resumeId)
                .map(email::equalsIgnoreCase)
                .orElse(false);
    }

    public AuthResponse register(RegisterRequest req) {
        log.info("Registering user with email: {}", req.email());
        if (userRepo.existsByEmail(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }

        User u = new User();
        u.setName(req.name());
        u.setEmail(req.email());
        // 👇 the actual encoding happens right here
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setRole(Role.USER);

        userRepo.save(u);
        String token = jwtService.generateToken(u.getId(), u.getEmail(), u.getRole().name());
        return new AuthResponse(token);
    }

    public AuthResponse login(LoginRequest req) {
        User u = userRepo.findByEmail(req.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(req.password(), u.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String token = jwtService.generateToken(u.getId(), u.getEmail(), u.getRole().name());
        return new AuthResponse(token);
    }
}

