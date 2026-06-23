package com.unimobili.agenda.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.unimobili.agenda.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityErrorIntegrationTest extends AbstractIntegrationTest {

    @Test
    void protectedRouteWithoutTokenReturnsStandardizedError() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity("/users", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        JsonNode body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status").asInt()).isEqualTo(401);
        assertThat(body.get("error").asText()).isNotBlank();
        assertThat(body.hasNonNull("timestamp")).isTrue();
        assertThat(body.get("path").asText()).isEqualTo("/users");
    }
}
