package com.jobhelper.shared.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Service
public class JwtService {

    private static final String HEADER = base64url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");

    private final byte[] secret;
    private final long expirySeconds;

    public JwtService(
            @Value("${jobhelper.jwt.secret}") String secret,
            @Value("${jobhelper.jwt.expiry-hours:24}") long expiryHours) {
        if (secret.length() < 32) {
            throw new IllegalStateException("jobhelper.jwt.secret must be at least 32 characters");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirySeconds = expiryHours * 3600;
    }

    public String generate(String username) {
        long now = Instant.now().getEpochSecond();
        String payload = base64url(
                "{\"sub\":\"" + username + "\",\"iat\":" + now + ",\"exp\":" + (now + expirySeconds) + "}");
        String data = HEADER + "." + payload;
        return data + "." + sign(data);
    }

    /** Returns the subject (username) if the token is valid and not expired, null otherwise. */
    public String validate(String token) {
        if (token == null || token.isBlank()) return null;
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3) return null;
        String data = parts[0] + "." + parts[1];
        if (!sign(data).equals(parts[2])) return null;
        try {
            String json = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            long exp = Long.parseLong(json.replaceAll(".*\"exp\":(\\d+).*", "$1"));
            if (Instant.now().getEpochSecond() > exp) return null;
            return json.replaceAll(".*\"sub\":\"([^\"]+)\".*", "$1");
        } catch (Exception e) {
            return null;
        }
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("JWT signing failed", e);
        }
    }

    private static String base64url(String json) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
