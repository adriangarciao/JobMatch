package com.adriangarciao.jobmatch.controller;

import com.adriangarciao.jobmatch.dto.AuthResponse;
import com.adriangarciao.jobmatch.dto.LoginRequest;
import com.adriangarciao.jobmatch.dto.RegisterRequest;
import com.adriangarciao.jobmatch.dto.TokenIntrospectionResponse;
import com.adriangarciao.jobmatch.model.RefreshToken;
import com.adriangarciao.jobmatch.model.Role;
import com.adriangarciao.jobmatch.model.User;
import com.adriangarciao.jobmatch.repository.UserRepository;
import com.adriangarciao.jobmatch.service.JwtService;
import com.adriangarciao.jobmatch.service.RefreshTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokens;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          RefreshTokenService refreshTokens) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokens = refreshTokens;
    }

    private void setRefreshCookie(HttpServletResponse res, String token, int maxAgeSec) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", token)
                .httpOnly(true)
                .secure(false)          // set true in prod (HTTPS)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(maxAgeSec)
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    // ---------- REGISTER ----------
    @PostMapping("/register")
    @Operation(summary = "Register a new user and return an access token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User created; access token returned"),
            @ApiResponse(responseCode = "400", description = "Validation error in the request body"),
            @ApiResponse(responseCode = "409", description = "Email already in use")
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req,
                                                 HttpServletResponse res) {
        log.info("Register endpoint called for email: {}", req.email());
        if (userRepository.existsByEmail(req.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new AuthResponse("Email already in use"));
        }

        User u = new User();
        u.setName(req.name());
        u.setEmail(req.email());
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setRole(Role.USER);
        userRepository.save(u);

        // issue access + refresh
        String access = jwtService.generateToken(u.getId(), u.getEmail(), u.getRole().name());
        var newRt = refreshTokens.create(u);              // ✅ this now exists
        setRefreshCookie(res, newRt.getToken(), refreshTokens.cookieMaxAgeSeconds());

        // keep your existing AuthResponse shape (token only)
        return ResponseEntity.ok(new AuthResponse(access));
    }

    // ---------- LOGIN ----------
    @PostMapping("/login")
    @Operation(summary = "Log in and return an access token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated; access token returned"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req,
                                              HttpServletResponse res) {
        User u = userRepository.findByEmail(req.email())
                .filter(x -> passwordEncoder.matches(req.password(), x.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        String access = jwtService.generateToken(u.getId(), u.getEmail(), u.getRole().name());

        var rt = refreshTokens.create(u);
        setRefreshCookie(res, rt.getToken(), refreshTokens.cookieMaxAgeSeconds());

        return ResponseEntity.ok(new AuthResponse(access));
    }

    // ---------- REFRESH (rotate) ----------
    // Will accept the HttpOnly cookie automatically. For manual testing you can also post { "refreshToken": "..." }
    @PostMapping("/refresh")
    @Operation(summary = "Rotate the refresh token and issue a new access token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New access token issued"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid, expired, or revoked refresh token")
    })
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = "refresh_token", required = false) String cookie,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletResponse res) {

        String presented = (cookie != null) ? cookie : (body == null ? null : body.get("refreshToken"));
        if (presented == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No refresh token");
        }

        RefreshToken newRt;
        try {
            newRt = refreshTokens.rotate(presented);
        } catch (IllegalArgumentException e) {
            // invalid / expired / revoked
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }

        User user = newRt.getUser();
        String access = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        setRefreshCookie(res, newRt.getToken(), refreshTokens.cookieMaxAgeSeconds());

        return ResponseEntity.ok(new AuthResponse(access));
    }

    // ---------- LOGOUT ----------
    @PostMapping("/logout")
    @Operation(summary = "Revoke the current user's refresh tokens and clear the cookie")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal(expression = "username") String email,
                                       HttpServletResponse res) {
        Optional<User> user = userRepository.findByEmail(email);
        user.ifPresent(refreshTokens::revokeUserTokens);

        // clear cookie
        setRefreshCookie(res, "", 0);
        return ResponseEntity.noContent().build();
    }

    // ---------- VERIFY (kept as you had it) ----------
    @GetMapping("/verify")
    @Operation(summary = "Introspect a bearer token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token is valid; claims returned"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid, or expired token")
    })
    public ResponseEntity<TokenIntrospectionResponse> verify(
            @RequestHeader(name = "Authorization", required = false) String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new TokenIntrospectionResponse(false, null, null, null, null, null,
                            "Missing or invalid Authorization header"));
        }
        String token = auth.substring("Bearer ".length());
        try {
            Jws<Claims> jws = jwtService.parse(token);
            Claims c = jws.getBody();

            String sub = c.getSubject();
            Long uid   = c.get("uid", Number.class) != null ? c.get("uid", Number.class).longValue() : null;
            String role= c.get("role", String.class);
            Instant iat= c.getIssuedAt() != null ? c.getIssuedAt().toInstant() : null;
            Instant exp= c.getExpiration() != null ? c.getExpiration().toInstant() : null;

            return ResponseEntity.ok(new TokenIntrospectionResponse(true, sub, uid, role, iat, exp, null));
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new TokenIntrospectionResponse(false, null, null, null, null,
                            e.getClaims().getExpiration().toInstant(), "Token expired"));
        } catch (io.jsonwebtoken.security.SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new TokenIntrospectionResponse(false, null, null, null, null, null, "Signature invalid"));
        } catch (io.jsonwebtoken.JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new TokenIntrospectionResponse(false, null, null, null, null, null,
                            "JWT invalid: " + e.getClass().getSimpleName()));
        }
    }
}
