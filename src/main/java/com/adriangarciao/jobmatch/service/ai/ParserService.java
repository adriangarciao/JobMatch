package com.adriangarciao.jobmatch.service.ai;

import com.adriangarciao.jobmatch.dto.ParsedJobDTO;
import com.adriangarciao.jobmatch.dto.ParsedResumeDTO;

/**
 * ParserService transforms raw resume and job posting text into internal structured DTOs.
 */
public interface ParserService {
    ParsedResumeDTO parseResume(String resumeText);

    ParsedJobDTO parseJob(String jobPostingText);
}
