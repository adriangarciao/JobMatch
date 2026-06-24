package com.adriangarciao.jobmatch.controller;

import com.adriangarciao.jobmatch.service.ResumeService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResumeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ResumeControllerTest.TestConfig.class)
class ResumeControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ResumeService resumeService;
    

    @Test
    void getResumeById_returnsOk() throws Exception {
        when(resumeService.get(anyLong())).thenReturn(Mockito.mock(com.adriangarciao.jobmatch.dto.ResumeDTO.class));
        mockMvc.perform(get("/api/resumes/1"))
                .andExpect(status().isOk());
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public ResumeService resumeService() { return Mockito.mock(ResumeService.class); }

        @Bean
        public com.adriangarciao.jobmatch.repository.UserRepository userRepository() {
            return Mockito.mock(com.adriangarciao.jobmatch.repository.UserRepository.class);
        }

        @Bean
        public com.adriangarciao.jobmatch.service.JwtService jwtService() {
            return Mockito.mock(com.adriangarciao.jobmatch.service.JwtService.class);
        }

        @Bean
        public com.adriangarciao.jobmatch.service.ai.ParserService parserService() {
            return Mockito.mock(com.adriangarciao.jobmatch.service.ai.ParserService.class);
        }
    }
}

