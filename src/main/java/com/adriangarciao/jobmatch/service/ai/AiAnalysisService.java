package com.adriangarciao.jobmatch.service.ai;
import com.adriangarciao.jobmatch.dto.*;
import com.adriangarciao.jobmatch.service.ai.llm.LLMService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.Objects;

@Service
public class AiAnalysisService {
    private static final Logger log = LoggerFactory.getLogger(AiAnalysisService.class);
    private final ParserService parserService;
    private final LLMService llmService;
    public AiAnalysisService(ParserService parserService, LLMService llmService) {
        this.parserService = Objects.requireNonNull(parserService, "parserService");
        this.llmService = Objects.requireNonNull(llmService, "llmService");
    }
    public FeedbackDTO analyze(SubmitAnalysisRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        
        ParsedResumeDTO parsedResume = parserService.parseResume(request.resumeText());
        ParsedJobDTO parsedJob = parserService.parseJob(request.jobPostingText());
        
        log.debug("Analyzing resume (len={}) against job (len={}), includeCoverLetter={}",
                parsedResume.rawText().length(), parsedJob.rawText().length(), request.includeCoverLetter());
        
        return llmService.generateFeedback(parsedResume, parsedJob, request.includeCoverLetter());
    }
}