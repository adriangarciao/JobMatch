package com.adriangarciao.jobmatch.controller;

import com.adriangarciao.jobmatch.service.UserService;
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

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(UserControllerTest.TestConfig.class)
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserService userService;
    @Autowired
    private com.adriangarciao.jobmatch.repository.UserRepository userRepository;
    @Autowired
    private com.adriangarciao.jobmatch.service.JwtService jwtService;

    @Test
    void getUserById_returnsOk() throws Exception {
        when(userService.getUserById(anyLong())).thenReturn(Mockito.mock(com.adriangarciao.jobmatch.dto.UserDTO.class));
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk());
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public UserService userService() { return Mockito.mock(UserService.class); }

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
