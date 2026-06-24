package com.adriangarciao.jobmatch.dto;

import jakarta.validation.constraints.*;

public record LoginRequest(
        @Email(message = "Email should be valid") @NotBlank(message = "Email is required") String email,
        @NotBlank(message = "Password is required") String password
) {
}
