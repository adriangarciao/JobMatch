package com.adriangarciao.jobmatch.dto;

import com.adriangarciao.jobmatch.model.ApplicationStatus;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record ApplicationCreateDTO(
        @NotBlank(message = "Job title is required") String jobTitle,
        @NotBlank(message = "Company is required") String company,
        @NotNull(message = "Status is required") ApplicationStatus status,
        @NotNull(message = "Applied date is required") LocalDate appliedDate,
        @Positive(message = "Compensation must be positive") Integer compensation,
        @NotNull(message = "User ID is required") Long userId
) { }
