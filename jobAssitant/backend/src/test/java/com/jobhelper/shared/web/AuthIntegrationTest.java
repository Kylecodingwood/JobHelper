package com.jobhelper.shared.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "jobhelper.auth.username=testuser",
        "jobhelper.auth.password=testpass",
        "jobhelper.jwt.secret=test-secret-that-is-long-enough-for-hmac"
})
class AuthIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    private String base() {
        return "http://localhost:" + port;
    }

    private HttpHeaders json() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    @Test
    void loginWithCorrectCredentials_returnsToken() {
        var res = rest.postForEntity(base() + "/api/v1/auth/login",
                new HttpEntity<>("{\"username\":\"testuser\",\"password\":\"testpass\"}", json()),
                java.util.Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).containsKey("token");
        assertThat(res.getBody()).containsKey("expiresAt");
    }

    @Test
    void loginWithWrongPassword_returns401() {
        var res = rest.postForEntity(base() + "/api/v1/auth/login",
                new HttpEntity<>("{\"username\":\"testuser\",\"password\":\"wrong\"}", json()),
                java.util.Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(res.getBody().get("code")).isEqualTo("INVALID_CREDENTIALS");
        assertThat(res.getBody()).containsKey("requestId");
    }

    @Test
    void loginWithMissingField_returns400() {
        var res = rest.postForEntity(base() + "/api/v1/auth/login",
                new HttpEntity<>("{\"username\":\"testuser\"}", json()),
                java.util.Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().get("code")).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void protectedEndpointWithoutToken_returns401() {
        var res = rest.getForEntity(base() + "/api/v1/jobs", String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedEndpointWithValidToken_returns200() {
        var loginRes = rest.postForEntity(base() + "/api/v1/auth/login",
                new HttpEntity<>("{\"username\":\"testuser\",\"password\":\"testpass\"}", json()),
                java.util.Map.class);
        String token = (String) loginRes.getBody().get("token");

        HttpHeaders auth = new HttpHeaders();
        auth.setBearerAuth(token);
        var res = rest.exchange(base() + "/api/v1/jobs", HttpMethod.GET,
                new HttpEntity<>(auth), String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
