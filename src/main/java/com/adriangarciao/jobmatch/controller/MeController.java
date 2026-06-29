package com.adriangarciao.jobmatch.controller;

import com.adriangarciao.jobmatch.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@Tag(name = "Users")
@SecurityRequirement(name = "bearerAuth")
public class  MeController {

        private static final Logger log = LoggerFactory.getLogger(MeController.class);
    private final UserRepository userRepository;
    public MeController(UserRepository userRepository){ this.userRepository = userRepository; }

    @GetMapping("/api/me")
    @Operation(summary = "Get the current user's id, email, name, and role")
    public ResponseEntity<?> me(Authentication auth) {
                log.info("Me endpoint called for user: {}", auth.getName());
        String email = auth.getName();  // principal is email from your filter
        return userRepository.findByEmail(email)
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(Map.of(
                        "id", u.getId(),
                        "email", u.getEmail(),
                        "name", u.getName(),
                        "role", u.getRole().name()
                )))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found")));
    }
}


