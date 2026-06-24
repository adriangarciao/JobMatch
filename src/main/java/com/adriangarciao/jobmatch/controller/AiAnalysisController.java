package com.adriangarciao.jobmatch.controller;

import com.adriangarciao.jobmatch.dto.FeedbackDTO;
import com.adriangarciao.jobmatch.dto.SubmitAnalysisRequest;
import com.adriangarciao.jobmatch.service.ai.AiAnalysisService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiAnalysisController {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisController.class);

    private final AiAnalysisService aiAnalysisService;

    public AiAnalysisController(AiAnalysisService aiAnalysisService) {
        this.aiAnalysisService = aiAnalysisService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<FeedbackDTO> analyze(@Valid @RequestBody SubmitAnalysisRequest request) {
        log.info("AI analysis endpoint called");
        FeedbackDTO feedback = aiAnalysisService.analyze(request);
        return ResponseEntity.ok(feedback);
    }
}
