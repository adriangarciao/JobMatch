package com.adriangarciao.jobmatch.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class UploadConfig {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Bean
    public CommandLineRunner createUploadDir() {
        return args -> Files.createDirectories(Path.of(uploadDir));
    }
}

