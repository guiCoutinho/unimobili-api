package com.unimobili.agenda.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.unimobili.agenda.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRefreshIntegrationTest extends AbstractIntegrationTest {

    @Test
    void refreshWithValidTokenReturnsNewAccessToken() {
        ResponseEntity<JsonNode> login = restTemplate.postForEntity(
                "/auth/login",
                Map.of("email", "gerente@unimobili.com", "senha", "admin12345"),
                JsonNode.class);
        String refreshToken = login.getBody().get("refreshToken").asText();

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/auth/refresh", Map.of("refreshToken", refreshToken), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("accessToken").asText()).isNotBlank();
    }

    @Test
    void refreshWithInvalidTokenReturns401() {
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/auth/refresh", Map.of("refreshToken", "nao-e-um-jwt-valido"), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
