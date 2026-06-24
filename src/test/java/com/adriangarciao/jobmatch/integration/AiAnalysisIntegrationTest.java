package com.adriangarciao.jobmatch.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test verifying the full AI analysis pipeline:
 * HTTP → AiAnalysisController → AiAnalysisService → ParserService → FakeLLMService
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class AiAnalysisIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "testuser@example.com")
    void analyzeResume_fullPipeline_returnsExpectedFeedback() throws Exception {
        String resumeText = """
                John Doe
                Skills: Java, Spring Boot, Docker, PostgreSQL, REST APIs
                
                Experience:
                Senior Software Engineer at TechCorp (2020-2023)
                - Built microservices using Spring Boot and Docker
                - Designed REST APIs consumed by 1M+ users
                - Optimized PostgreSQL queries for 10x performance improvement
                """;

        String jobPostingText = """
                Title: Senior Backend Engineer
                
                Requirements:
                - 5+ years experience with Java
                - Strong knowledge of Spring Boot
                - Experience with Docker and Kubernetes
                - PostgreSQL or MySQL database skills
                
                Nice to have:
                - AWS experience
                - GraphQL knowledge
                """;

        String requestJson = String.format(
                "{\"resumeText\":\"%s\",\"jobPostingText\":\"%s\",\"includeCoverLetter\":true}",
                escapeJson(resumeText),
                escapeJson(jobPostingText)
        );

        mockMvc.perform(post("/api/ai/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchScore").isNumber())
                .andExpect(jsonPath("$.matchScore").value(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(0),
                        org.hamcrest.Matchers.lessThanOrEqualTo(100)
                )))
                .andExpect(jsonPath("$.strengths").isArray())
                .andExpect(jsonPath("$.strengths").isNotEmpty())
                .andExpect(jsonPath("$.weaknesses").isArray())
                .andExpect(jsonPath("$.suggestions").isArray())
                .andExpect(jsonPath("$.suggestions").isNotEmpty())
                .andExpect(jsonPath("$.summary").isString())
                .andExpect(jsonPath("$.summary").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "testuser@example.com")
    void analyzeResume_lowMatch_reflectsInScore() throws Exception {
        String resumeText = """
                Jane Smith
                Skills: Python, Django, React, MongoDB
                
                Experience:
                Full Stack Developer at WebCo (2021-2023)
                - Built web apps with Django and React
                - Managed MongoDB databases
                """;

        String jobPostingText = """
                Title: Java Enterprise Architect
                
                Requirements:
                - Expert-level Java and Spring Framework
                - Microservices architecture experience
                - Kubernetes and Docker orchestration
                - Oracle or PostgreSQL database skills
                """;

        String requestJson = String.format(
                "{\"resumeText\":\"%s\",\"jobPostingText\":\"%s\",\"includeCoverLetter\":false}",
                escapeJson(resumeText),
                escapeJson(jobPostingText)
        );

        mockMvc.perform(post("/api/ai/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchScore").isNumber())
                .andExpect(jsonPath("$.matchScore").value(org.hamcrest.Matchers.lessThan(50)))
                .andExpect(jsonPath("$.weaknesses").isArray())
                .andExpect(jsonPath("$.weaknesses").isNotEmpty())
                .andExpect(jsonPath("$.suggestions").isArray())
                .andExpect(jsonPath("$.summary").value(org.hamcrest.Matchers.containsString("fit")));
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
