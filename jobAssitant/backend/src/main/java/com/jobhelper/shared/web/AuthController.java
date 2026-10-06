package com.jobhelper.shared.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final String configuredUsername;
    private final String configuredPassword;
    private final JwtService jwtService;

    public AuthController(
            @Value("${jobhelper.auth.username}") String configuredUsername,
            @Value("${jobhelper.auth.password}") String configuredPassword,
            JwtService jwtService) {
        this.configuredUsername = configuredUsername;
        this.configuredPassword = configuredPassword;
        this.jwtService = jwtService;
    }

    public record LoginRequest(
            @NotBlank @Size(max = 100) String username,
            @NotBlank @Size(max = 200) String password) {}

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest req) {
        if (!configuredUsername.equals(req.username()) || !configuredPassword.equals(req.password())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid username or password");
        }
        String token = jwtService.generate(req.username());
        return Map.of(
                "token", token,
                "expiresAt", Instant.now().plus(24, ChronoUnit.HOURS).toString());
    }
}
