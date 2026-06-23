package com.unimobili.agenda.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.unimobili.agenda.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UsersCrudIntegrationTest extends AbstractIntegrationTest {

    @Test
    void gerenteCreatesUser() {
        Map<String, Object> request = Map.of(
                "nome", "Maria Externa",
                "email", "maria.externa@unimobili.com",
                "senha", "senha12345",
                "telefone", "11999990000",
                "cargo", "Consultora",
                "role", "EXTERNO"
        );

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/users", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders(gerenteToken())),
                JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("id").asText()).isNotBlank();
        assertThat(response.getBody().get("email").asText()).isEqualTo("maria.externa@unimobili.com");
        assertThat(response.getBody().get("role").asText()).isEqualTo("EXTERNO");
        assertThat(response.getBody().has("senha")).isFalse();
    }

    @Test
    void nonGerenteCannotManageUsers() {
        createUser(gerenteToken(), "Interno Acesso", "interno.acesso@unimobili.com", "senha12345", Role.INTERNO);
        String internoToken = login("interno.acesso@unimobili.com", "senha12345");

        Map<String, Object> request = Map.of(
                "nome", "Alguem", "email", "alguem@unimobili.com",
                "senha", "senha12345", "role", "EXTERNO");
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/users", HttpMethod.POST, new HttpEntity<>(request, authHeaders(internoToken)), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void duplicateEmailReturns409() {
        createUser(gerenteToken(), "Dup Um", "dup@unimobili.com", "senha12345", Role.EXTERNO);

        ResponseEntity<JsonNode> second = restTemplate.exchange(
                "/users", HttpMethod.POST,
                new HttpEntity<>(Map.of("nome", "Dup Dois", "email", "dup@unimobili.com",
                        "senha", "senha12345", "role", "EXTERNO"), authHeaders(gerenteToken())),
                JsonNode.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void shortPasswordReturns400() {
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/users", HttpMethod.POST,
                new HttpEntity<>(Map.of("nome", "Curta", "email", "curta@unimobili.com",
                        "senha", "123", "role", "EXTERNO"), authHeaders(gerenteToken())),
                JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("status").asInt()).isEqualTo(400);
    }

    @Test
    void listUsersIsPaginated() {
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/users?page=0&size=5", HttpMethod.GET,
                new HttpEntity<>(authHeaders(gerenteToken())), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("content").isArray()).isTrue();
        assertThat(response.getBody().get("page").get("totalElements").asInt()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void getByIdReturnsUserAndUnknownReturns404() {
        JsonNode created = createUser(gerenteToken(), "Get Alvo", "get.alvo@unimobili.com", "senha12345", Role.EXTERNO);
        String id = created.get("id").asText();

        ResponseEntity<JsonNode> found = restTemplate.exchange(
                "/users/" + id, HttpMethod.GET, new HttpEntity<>(authHeaders(gerenteToken())), JsonNode.class);
        assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(found.getBody().get("email").asText()).isEqualTo("get.alvo@unimobili.com");

        ResponseEntity<JsonNode> missing = restTemplate.exchange(
                "/users/" + UUID.randomUUID(), HttpMethod.GET,
                new HttpEntity<>(authHeaders(gerenteToken())), JsonNode.class);
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateChangesFieldsButNotPassword() {
        JsonNode created = createUser(gerenteToken(), "Update Alvo", "update.alvo@unimobili.com", "senha12345", Role.EXTERNO);
        String id = created.get("id").asText();

        Map<String, Object> update = Map.of(
                "nome", "Update Alvo Renomeado", "email", "update.alvo@unimobili.com",
                "telefone", "11888887777", "cargo", "Nova Funcao", "role", "EXTERNO", "ativo", true);
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/users/" + id, HttpMethod.PUT, new HttpEntity<>(update, authHeaders(gerenteToken())), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("nome").asText()).isEqualTo("Update Alvo Renomeado");
        assertThat(response.getBody().get("telefone").asText()).isEqualTo("11888887777");
        // senha não muda no PUT: login com a senha original continua funcionando
        ResponseEntity<JsonNode> login = restTemplate.postForEntity(
                "/auth/login", Map.of("email", "update.alvo@unimobili.com", "senha", "senha12345"), JsonNode.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void deleteSoftDeletesUser() {
        JsonNode created = createUser(gerenteToken(), "Delete Alvo", "delete.alvo@unimobili.com", "senha12345", Role.EXTERNO);
        String id = created.get("id").asText();

        ResponseEntity<Void> delete = restTemplate.exchange(
                "/users/" + id, HttpMethod.DELETE, new HttpEntity<>(authHeaders(gerenteToken())), Void.class);
        assertThat(delete.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<JsonNode> found = restTemplate.exchange(
                "/users/" + id, HttpMethod.GET, new HttpEntity<>(authHeaders(gerenteToken())), JsonNode.class);
        assertThat(found.getBody().get("ativo").asBoolean()).isFalse();
    }

    @Test
    void auditingPopulatesCreatedByAndCreatedAt() {
        String token = gerenteToken();
        ResponseEntity<JsonNode> me = restTemplate.exchange(
                "/auth/me", HttpMethod.GET, new HttpEntity<>(authHeaders(token)), JsonNode.class);
        String gerenteId = me.getBody().get("id").asText();

        JsonNode created = createUser(token, "Audit Alvo", "audit.alvo@unimobili.com", "senha12345", Role.EXTERNO);

        assertThat(created.hasNonNull("createdAt")).isTrue();
        assertThat(created.get("createdBy").asText()).isEqualTo(gerenteId);
    }

    // ---- helpers ----

    protected JsonNode createUser(String token, String nome, String email, String senha, Role role) {
        Map<String, Object> request = Map.of(
                "nome", nome, "email", email, "senha", senha, "role", role.name());
        return restTemplate.exchange(
                "/users", HttpMethod.POST, new HttpEntity<>(request, authHeaders(token)), JsonNode.class).getBody();
    }

    protected String gerenteToken() {
        return login("gerente@unimobili.com", "admin12345");
    }

    protected String login(String email, String senha) {
        ResponseEntity<JsonNode> login = restTemplate.postForEntity(
                "/auth/login", Map.of("email", email, "senha", senha), JsonNode.class);
        return login.getBody().get("accessToken").asText();
    }

    protected HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
