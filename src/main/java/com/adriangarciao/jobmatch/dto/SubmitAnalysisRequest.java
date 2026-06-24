package com.adriangarciao.jobmatch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for synchronous analysis of a resume against a job posting.
 */
public record SubmitAnalysisRequest(
        @NotBlank(message = "resumeText is required")
        @Size(max = 20000, message = "resumeText must be at most 20000 characters")
        String resumeText,

        @NotBlank(message = "jobPostingText is required")
        @Size(max = 20000, message = "jobPostingText must be at most 20000 characters")
        String jobPostingText,

        boolean includeCoverLetter
) {
}
