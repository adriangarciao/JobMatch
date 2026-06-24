package com.adriangarciao.jobmatch.controller;

import com.adriangarciao.jobmatch.service.ApplicationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApplicationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApplicationControllerTest.TestConfig.class)
class ApplicationControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private com.adriangarciao.jobmatch.repository.UserRepository userRepository;
    

    @Test
    void getApplicationById_returnsOk() throws Exception {
        when(applicationService.getOwned(anyLong(), anyLong())).thenReturn(Mockito.mock(com.adriangarciao.jobmatch.dto.ApplicationDTO.class));
        var user = Mockito.mock(com.adriangarciao.jobmatch.model.User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getEmail()).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(java.util.Optional.of(user));

        org.springframework.security.core.Authentication auth = Mockito.mock(org.springframework.security.core.Authentication.class);
        Mockito.when(auth.getName()).thenReturn("test@example.com");

        mockMvc.perform(get("/api/applications/1").principal(auth))
                .andExpect(status().isOk());
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public ApplicationService applicationService() { return Mockito.mock(ApplicationService.class); }

        @Bean
        public com.adriangarciao.jobmatch.repository.UserRepository userRepository() {
            return Mockito.mock(com.adriangarciao.jobmatch.repository.UserRepository.class);
        }

        @Bean
        public com.adriangarciao.jobmatch.service.JwtService jwtService() {
            return Mockito.mock(com.adriangarciao.jobmatch.service.JwtService.class);
        }
    }

}
