package com.adriangarciao.jobmatch.controller;

import com.adriangarciao.jobmatch.dto.FeedbackDTO;
import com.adriangarciao.jobmatch.service.ai.AiAnalysisService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiAnalysisController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(AiAnalysisControllerTest.TestConfig.class)
class AiAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AiAnalysisService aiAnalysisService;

    @Test
    void analyze_validRequest_returnsOkAndBody() throws Exception {
        FeedbackDTO feedback = new FeedbackDTO(82, List.of("skill match"), List.of("gap"), List.of("improve"), "summary text", null, null);
        when(aiAnalysisService.analyze(any())).thenReturn(feedback);

        String json = "{\"resumeText\":\"My resume contents\",\"jobPostingText\":\"Some job posting\",\"includeCoverLetter\":true}";
        mockMvc.perform(post("/api/ai/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchScore").value(82))
                .andExpect(jsonPath("$.strengths[0]").value("skill match"))
                .andExpect(jsonPath("$.weaknesses[0]").value("gap"))
                .andExpect(jsonPath("$.suggestions[0]").value("improve"))
                .andExpect(jsonPath("$.summary").value("summary text"));
    }

    @Test
    void analyze_invalidRequest_blankResume_returnsBadRequest() throws Exception {
        String invalidJson = "{\"resumeText\":\"\",\"jobPostingText\":\"Some job posting\"}"; // missing includeCoverLetter defaults false
        mockMvc.perform(post("/api/ai/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public AiAnalysisService aiAnalysisService() { return Mockito.mock(AiAnalysisService.class); }

        // Provide typical extra beans to satisfy potential security or component expectations
        @Bean
        public com.adriangarciao.jobmatch.service.JwtService jwtService() {
            return Mockito.mock(com.adriangarciao.jobmatch.service.JwtService.class);
        }

        @Bean
        public com.adriangarciao.jobmatch.repository.UserRepository userRepository() {
            return Mockito.mock(com.adriangarciao.jobmatch.repository.UserRepository.class);
        }
    }
}
