package com.adriangarciao.jobmatch.dto;

import java.time.LocalDateTime;

public record ResumeDTO(
        Long id,
        Long userId,
        String originalFilename,
        String contentType,
        Long sizeBytes,
        LocalDateTime uploadedAt
        , String parsedText
) {}
