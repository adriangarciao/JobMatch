package com.adriangarciao.jobmatch.dto;

import java.util.List;

import jakarta.validation.constraints.*;

public record UserUpdateDTO(
        @Size(min = 1, max = 100, message = "Name must be 1-100 characters") String name,
        @Email(message = "Email should be valid") String email,
        @Min(value = 16, message = "Age must be at least 16") Integer age,
        @Positive(message = "Graduation year must be valid") Integer gradYear,
        @Size(max = 100, message = "College name too long") String college,
        @Size(max = 100, message = "Major name too long") String major,
        List<@NotBlank String> desiredJobTitle,
        List<@NotBlank String> skills,
        List<@NotBlank String> experience
) {
}
