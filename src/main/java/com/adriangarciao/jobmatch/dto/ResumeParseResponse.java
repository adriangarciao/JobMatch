package com.adriangarciao.jobmatch.dto;

import java.util.List;

public record ResumeParseResponse(
        String rawText,
        List<String> skills,
        List<String> experiences
) {}
