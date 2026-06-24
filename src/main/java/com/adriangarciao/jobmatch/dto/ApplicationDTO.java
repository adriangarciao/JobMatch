package com.adriangarciao.jobmatch.dto;

import com.adriangarciao.jobmatch.model.ApplicationStatus;

import java.time.LocalDate;

public record ApplicationDTO (
        Long id,
        String jobTitle,
        String company,
        ApplicationStatus status,
        LocalDate appliedDate,
        Integer compensation,
        Long userId
){}
