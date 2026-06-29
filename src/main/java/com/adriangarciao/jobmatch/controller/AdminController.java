package com.adriangarciao.jobmatch.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    @GetMapping("/ping")
    @Operation(summary = "Admin-only health ping")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String,String> ping() {
        log.info("Admin ping endpoint called");
        return Map.of("ok", "admin-only");
    }
}
