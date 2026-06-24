package com.adriangarciao.jobmatch.dto;

import java.util.List;
import java.util.Objects;

/**
 * Internal DTO representing a parsed job posting. Not exposed directly via API.
 */
public record ParsedJobDTO(
        String title,
        List<String> requiredSkills,
        List<String> niceToHaveSkills,
        String jobLocation,
        String compensationInfo,
        String rawText
) {
    public ParsedJobDTO {
        requiredSkills = (requiredSkills == null) ? List.of() : List.copyOf(requiredSkills);
        niceToHaveSkills = (niceToHaveSkills == null) ? List.of() : List.copyOf(niceToHaveSkills);
        title = Objects.requireNonNullElse(title, "");
        rawText = Objects.requireNonNullElse(rawText, "");
        // jobLocation and compensationInfo can be null if not found
    }
}
