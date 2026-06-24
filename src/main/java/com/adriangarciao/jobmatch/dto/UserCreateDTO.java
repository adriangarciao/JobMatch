package com.adriangarciao.jobmatch.dto;

import jakarta.validation.constraints.*;

import java.util.List;

public record UserCreateDTO(
        @NotBlank(message = "Name is required") String name,
        @Email(message = "Email should be valid") @NotBlank(message = "Email is required") String email,
        @Min(value = 16, message = "Age must be at least 16") int age,
        @Positive(message = "Graduation year must be valid") int gradYear,
        String college,
        String major,
        List<String> desiredJobTitle,
        List<String> skills,
        List<String> experience
) {
}
