package com.unimobili.agenda.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.unimobili.agenda.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthMeIntegrationTest extends AbstractIntegrationTest {

    @Test
    void meWithValidTokenReturnsCurrentUser() {
        String token = login("gerente@unimobili.com", "admin12345");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/auth/me", HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("email").asText()).isEqualTo("gerente@unimobili.com");
        assertThat(response.getBody().get("role").asText()).isEqualTo("GERENTE");
    }

    @Test
    void meWithoutTokenReturns401() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity("/auth/me", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private String login(String email, String senha) {
        ResponseEntity<JsonNode> login = restTemplate.postForEntity(
                "/auth/login", Map.of("email", email, "senha", senha), JsonNode.class);
        return login.getBody().get("accessToken").asText();
    }
}
