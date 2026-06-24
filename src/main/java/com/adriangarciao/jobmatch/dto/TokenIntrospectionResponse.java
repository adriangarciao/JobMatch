package com.adriangarciao.jobmatch.dto;

import java.time.Instant;

public record TokenIntrospectionResponse(
        boolean valid,
        String subject,
        Long uid,
        String role,
        Instant issuedAt,
        Instant expiresAt,
        String error
) {}
