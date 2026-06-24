package com.adriangarciao.jobmatch.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank(message = "Name is required") String name,
        @Email(message = "Email should be valid") @NotBlank(message = "Email is required") String email,
        @Size(min = 8, max = 128, message = "Password must be 8-128 characters") String password
) {
}
