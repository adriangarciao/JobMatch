package com.adriangarciao.jobmatch.service.ai;

import com.adriangarciao.jobmatch.dto.FeedbackDTO;
import com.adriangarciao.jobmatch.dto.ParsedJobDTO;
import com.adriangarciao.jobmatch.dto.ParsedResumeDTO;
import com.adriangarciao.jobmatch.dto.SubmitAnalysisRequest;
import com.adriangarciao.jobmatch.service.ai.llm.LLMService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiAnalysisServiceTest {

    private ParserService parserService;
    private LLMService llmService;
    private AiAnalysisService aiAnalysisService;

    @BeforeEach
    void setUp() {
        parserService = Mockito.mock(ParserService.class);
        llmService = Mockito.mock(LLMService.class);
        aiAnalysisService = new AiAnalysisService(parserService, llmService);
    }

    @Test
    void analyze_validRequest_delegatesAndReturnsFeedback() {
        SubmitAnalysisRequest req = new SubmitAnalysisRequest("John Doe resume text", "Great job posting", false);

        ParsedResumeDTO parsedResume = new ParsedResumeDTO("John Doe", List.of("java"), List.of("exp1"), req.resumeText());
        ParsedJobDTO parsedJob = new ParsedJobDTO("Engineer", List.of("java"), List.of(), req.jobPostingText(), null, null);
        FeedbackDTO feedback = new FeedbackDTO(75, List.of("strong java"), List.of(), List.of("add cover letter"), "summary", null, null);

        when(parserService.parseResume(req.resumeText())).thenReturn(parsedResume);
        when(parserService.parseJob(req.jobPostingText())).thenReturn(parsedJob);
        when(llmService.generateFeedback(parsedResume, parsedJob, req.includeCoverLetter())).thenReturn(feedback);

        FeedbackDTO result = aiAnalysisService.analyze(req);

        assertNotNull(result);
        assertEquals(75, result.matchScore());
        assertEquals("summary", result.summary());
        verify(parserService, times(1)).parseResume(req.resumeText());
        verify(parserService, times(1)).parseJob(req.jobPostingText());
        verify(llmService, times(1)).generateFeedback(parsedResume, parsedJob, req.includeCoverLetter());
    }

    @Test
    void analyze_nullRequest_throws() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> aiAnalysisService.analyze(null));
        assertTrue(ex.getMessage() == null || ex.getMessage().toLowerCase().contains("request"));
        verifyNoInteractions(parserService, llmService);
    }
}
