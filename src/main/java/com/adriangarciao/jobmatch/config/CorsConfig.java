package com.adriangarciao.jobmatch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    /**
     * Allowed CORS origin patterns. Override per environment with the
     * APP_CORS_ALLOWED_ORIGINS env var (comma-separated). Patterns support
     * wildcards (e.g. https://*.vercel.app) because we use
     * setAllowedOriginPatterns, which — unlike setAllowedOrigins("*") — is
     * compatible with allowCredentials(true).
     *
     * Defaults cover local dev, Vercel production + preview deploys, and Railway.
     */
    @Value("${app.cors.allowed-origins:"
            + "http://localhost:5173,"
            + "http://localhost:3000,"
            + "https://*.vercel.app,"
            + "https://*.railway.app,"
            + "https://*.up.railway.app}")
    private List<String> allowedOriginPatterns;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(allowedOriginPatterns);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
