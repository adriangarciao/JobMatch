package com.adriangarciao.jobmatch.config;


import com.adriangarciao.jobmatch.JWTUtility.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity // enables @PreAuthorize on services
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // It’s an API: no CSRF tokens
                .csrf(csrf -> csrf.disable())

                // CORS doesn’t affect Postman, but fine to allow all for now
                .cors(Customizer.withDefaults())

                // Stateless JWT
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // What's allowed without a token
                .authorizeHttpRequests(auth -> auth
                        // OpenAPI JSON/YAML + Swagger UI (springdoc defaults). Only the docs
                        // are opened up; protected endpoints below still require a valid JWT.
                        .requestMatchers(
                                "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml",
                                "/swagger-ui.html", "/swagger-ui/**"
                        ).permitAll()

                        // simplest: open all auth endpoints
                        .requestMatchers("/api/auth/**").permitAll()

                        // Allow AI analysis endpoint without auth for demo
                        .requestMatchers("/api/ai/**").permitAll()

                        // admin protected
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // everything else needs auth
                        .anyRequest().authenticated()
                )

                // Put our JWT filter in the chain
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}


