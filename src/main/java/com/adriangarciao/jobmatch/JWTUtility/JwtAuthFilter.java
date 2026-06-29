package com.adriangarciao.jobmatch.JWTUtility;

import com.adriangarciao.jobmatch.security.AppPrincipal;
import com.adriangarciao.jobmatch.service.JwtService;
import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /** Never attempt token validation on the public API docs (OpenAPI JSON/YAML + Swagger UI). */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest req) {
        String uri = req.getRequestURI();
        return uri.startsWith("/v3/api-docs")
                || uri.startsWith("/swagger-ui")
                || uri.equals("/swagger-ui.html");
    }

        @Override
        protected void doFilterInternal(@NonNull HttpServletRequest req, @NonNull HttpServletResponse res, @NonNull FilterChain chain)
            throws ServletException, IOException {

        // 1) Let CORS preflight pass
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(req, res);
            return;
        }

        // 2) Skip auth endpoints (they’re permitAll and don’t need JWT auth)
        String uri = req.getRequestURI();
        if (uri.startsWith("/api/auth/")) {
            chain.doFilter(req, res);
            return;
        }

        // 3) If already authenticated, continue
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(req, res);
            return;
        }

        // 4) Expect Bearer token
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            chain.doFilter(req, res);
            return;
        }

        String token = auth.substring(7);
        try {
            var jws = jwtService.parse(token);
            var body = jws.getBody();

            String email = body.getSubject();
            Number uidNum = body.get("uid", Number.class);
            Long uid = (uidNum != null) ? uidNum.longValue() : null;  // should be present, but be defensive
            String role = body.get("role", String.class);
            if (email == null || uid == null || role == null) {
                // malformed token → skip auth
                log.warn("JWT missing required claims: email={}, uid={}, role={}", email, uid, role);
                chain.doFilter(req, res);
                return;
            }

            var principal = new AppPrincipal(uid, email, role); // your record: (id, email, role)
            var authorities = java.util.List.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_" + role));

            var authToken = new UsernamePasswordAuthenticationToken(principal, null, authorities);
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
            SecurityContextHolder.getContext().setAuthentication(authToken);

        } catch (io.jsonwebtoken.JwtException e) {
            // invalid/expired → leave unauthenticated
            log.warn("Invalid or expired JWT: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(req, res);
    }
}