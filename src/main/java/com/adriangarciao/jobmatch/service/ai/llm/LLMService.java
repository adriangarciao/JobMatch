package com.adriangarciao.jobmatch.service.ai.llm;

import com.adriangarciao.jobmatch.dto.FeedbackDTO;
import com.adriangarciao.jobmatch.dto.ParsedJobDTO;
import com.adriangarciao.jobmatch.dto.ParsedResumeDTO;

/**
 * Abstraction for generating AI-style feedback from parsed resume and job posting.
 */
public interface LLMService {
    FeedbackDTO generateFeedback(ParsedResumeDTO resume, ParsedJobDTO job, boolean includeCoverLetter);
}
