package com.adriangarciao.jobmatch.dto;

import java.util.List;
import java.util.Objects;

/**
 * Response DTO returned by the synchronous analysis endpoint.
 */
public record FeedbackDTO(
        int matchScore,
        List<String> strengths,
        List<String> weaknesses,
        List<String> suggestions,
        String summary,
        String jobLocation,
        String compensationInfo
) {
    public FeedbackDTO {
        // Normalize lists to non-null immutable lists
        strengths = (strengths == null) ? List.of() : List.copyOf(strengths);
        weaknesses = (weaknesses == null) ? List.of() : List.copyOf(weaknesses);
        suggestions = (suggestions == null) ? List.of() : List.copyOf(suggestions);
        summary = Objects.requireNonNullElse(summary, "");
        // jobLocation and compensationInfo can be null if not found
    }
}
