package com.adriangarciao.jobmatch.controller;

import com.adriangarciao.jobmatch.service.UserService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

 
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(AdminControllerTest.TestConfig.class)
class AdminControllerTest {
    @Autowired
    private MockMvc mockMvc;
    

    @Test
    void ping_returnsOk() throws Exception {
        mockMvc.perform(get("/api/admin/ping"))
                .andExpect(status().isOk());
    }
    
    @TestConfiguration
    static class TestConfig {
        @Bean
        public UserService userService() { return Mockito.mock(UserService.class); }

        @Bean
        public com.adriangarciao.jobmatch.service.JwtService jwtService() {
            return Mockito.mock(com.adriangarciao.jobmatch.service.JwtService.class);
        }
    }

}
