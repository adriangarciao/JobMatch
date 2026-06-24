package com.adriangarciao.jobmatch.dto;

import java.util.List;
import java.util.Objects;

/**
 * Internal DTO representing a parsed resume. Not exposed directly via API.
 */
public record ParsedResumeDTO(
        String redactedName,
        List<String> skills,
        List<String> experiences,
        String rawText
) {
    public ParsedResumeDTO {
        skills = (skills == null) ? List.of() : List.copyOf(skills);
        experiences = (experiences == null) ? List.of() : List.copyOf(experiences);
        redactedName = Objects.requireNonNullElse(redactedName, "");
        rawText = Objects.requireNonNullElse(rawText, "");
    }
}
