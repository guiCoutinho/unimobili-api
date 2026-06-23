package com.unimobili.agenda.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.unimobili.agenda.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthLoginIntegrationTest extends AbstractIntegrationTest {

    @Test
    void loginWithSeededGerenteReturnsAccessToken() {
        Map<String, String> credentials = Map.of(
                "email", "gerente@unimobili.com",
                "senha", "admin12345"
        );

        ResponseEntity<JsonNode> response =
                restTemplate.postForEntity("/auth/login", credentials, JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("accessToken").asText()).isNotBlank();
    }

    @Test
    void loginWithWrongPasswordReturns401() {
        Map<String, String> credentials = Map.of(
                "email", "gerente@unimobili.com",
                "senha", "senha-errada"
        );

        ResponseEntity<JsonNode> response =
                restTemplate.postForEntity("/auth/login", credentials, JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
