package com.unimobili.agenda;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiDocsIntegrationTest extends AbstractIntegrationTest {

    @Test
    void openApiDocsAreAvailable() {
        ResponseEntity<String> response = restTemplate.getForEntity("/v3/api-docs", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void openApiDocumentsJwtSchemeAndTitle() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity("/v3/api-docs", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("info").get("title").asText())
                .isEqualTo("API de Agenda Corporativa");
        assertThat(response.getBody().get("components").get("securitySchemes").has("bearerAuth"))
                .isTrue();
    }
}
