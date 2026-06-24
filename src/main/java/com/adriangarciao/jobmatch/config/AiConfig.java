package com.adriangarciao.jobmatch.config;

import com.adriangarciao.jobmatch.service.ai.llm.FakeLLMService;
import com.adriangarciao.jobmatch.service.ai.llm.LLMService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public LLMService llmService() {
        return new FakeLLMService();
    }
}
