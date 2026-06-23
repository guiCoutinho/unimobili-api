package com.unimobili.agenda.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.unimobili.agenda.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EventsIntegrationTest extends AbstractIntegrationTest {

    @Test
    void internoCreatesEvent() {
        String gerente = gerenteToken();
        String externoId = createUser(gerente, "Externo Evento", "externo.evento@unimobili.com", "EXTERNO");
        createUser(gerente, "Interno Evento", "interno.evento@unimobili.com", "INTERNO");
        String interno = login("interno.evento@unimobili.com", "senha12345");

        Instant inicio = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        Instant fim = inicio.plus(1, ChronoUnit.HOURS);

        Map<String, Object> request = eventRequest("Reunião", externoId, inicio, fim);
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/events", HttpMethod.POST, new HttpEntity<>(request, authHeaders(interno)), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("id").asText()).isNotBlank();
        assertThat(response.getBody().get("status").asText()).isEqualTo("AGENDADO");
        assertThat(response.getBody().get("externalUserId").asText()).isEqualTo(externoId);
    }

    @Test
    void externoCannotCreateEvent() {
        String gerente = gerenteToken();
        String externoId = createUser(gerente, "Ext Bloqueio", "ext.bloqueio@unimobili.com", "EXTERNO");
        String externoToken = login("ext.bloqueio@unimobili.com", "senha12345");

        Instant inicio = future();
        ResponseEntity<JsonNode> response = postEvent(externoToken,
                eventRequest("Tentativa", externoId, inicio, inicio.plus(1, ChronoUnit.HOURS)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void eventForNonExternoUserReturns400() {
        String gerente = gerenteToken();
        String internoId = createUser(gerente, "Int Alvo", "int.alvo400@unimobili.com", "INTERNO");

        Instant inicio = future();
        ResponseEntity<JsonNode> response = postEvent(gerente,
                eventRequest("Invalido", internoId, inicio, inicio.plus(1, ChronoUnit.HOURS)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void endBeforeStartReturns400() {
        String gerente = gerenteToken();
        String externoId = createUser(gerente, "Ext FimInicio", "ext.fiminicio@unimobili.com", "EXTERNO");

        Instant inicio = future();
        ResponseEntity<JsonNode> response = postEvent(gerente,
                eventRequest("Invertido", externoId, inicio, inicio.minus(1, ChronoUnit.HOURS)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void pastEventReturns400() {
        String gerente = gerenteToken();
        String externoId = createUser(gerente, "Ext Passado", "ext.passado@unimobili.com", "EXTERNO");

        Instant inicio = Instant.now().minus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        ResponseEntity<JsonNode> response = postEvent(gerente,
                eventRequest("Passado", externoId, inicio, inicio.plus(1, ChronoUnit.HOURS)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void overlappingEventReturns409AndAdjacentIsAllowed() {
        String gerente = gerenteToken();
        String externoId = createUser(gerente, "Ext Conflito", "ext.conflito@unimobili.com", "EXTERNO");

        Instant inicio = future();
        Instant fim = inicio.plus(1, ChronoUnit.HOURS);
        assertThat(postEvent(gerente, eventRequest("Primeiro", externoId, inicio, fim))
                .getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // sobreposto -> 409
        ResponseEntity<JsonNode> overlap = postEvent(gerente, eventRequest("Sobreposto", externoId,
                inicio.plus(30, ChronoUnit.MINUTES), fim.plus(30, ChronoUnit.MINUTES)));
        assertThat(overlap.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // adjacente (começa quando o outro termina) -> 201
        ResponseEntity<JsonNode> adjacent = postEvent(gerente, eventRequest("Adjacente", externoId,
                fim, fim.plus(1, ChronoUnit.HOURS)));
        assertThat(adjacent.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void listEventsIsPaginated() {
        String gerente = gerenteToken();
        String externoId = createUser(gerente, "Ext Lista", "ext.lista@unimobili.com", "EXTERNO");
        Instant inicio = future();
        postEvent(gerente, eventRequest("Para Listar", externoId, inicio, inicio.plus(1, ChronoUnit.HOURS)));

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/events?page=0&size=5", HttpMethod.GET, new HttpEntity<>(authHeaders(gerente)), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("content").isArray()).isTrue();
        assertThat(response.getBody().get("page").get("totalElements").asInt()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void getByIdReturnsEventAndUnknownReturns404() {
        String gerente = gerenteToken();
        String externoId = createUser(gerente, "Ext Detalhe", "ext.detalhe@unimobili.com", "EXTERNO");
        Instant inicio = future();
        String id = postEvent(gerente, eventRequest("Detalhe", externoId, inicio, inicio.plus(1, ChronoUnit.HOURS)))
                .getBody().get("id").asText();

        ResponseEntity<JsonNode> found = restTemplate.exchange(
                "/events/" + id, HttpMethod.GET, new HttpEntity<>(authHeaders(gerente)), JsonNode.class);
        assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(found.getBody().get("titulo").asText()).isEqualTo("Detalhe");

        ResponseEntity<JsonNode> missing = restTemplate.exchange(
                "/events/" + java.util.UUID.randomUUID(), HttpMethod.GET,
                new HttpEntity<>(authHeaders(gerente)), JsonNode.class);
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createdByIsTheAuthenticatedCreator() {
        String gerente = gerenteToken();
        String externoId = createUser(gerente, "Ext Autor", "ext.autor@unimobili.com", "EXTERNO");
        createUser(gerente, "Int Autor", "int.autor@unimobili.com", "INTERNO");
        String interno = login("int.autor@unimobili.com", "senha12345");
        String internoId = restTemplate.exchange("/auth/me", HttpMethod.GET,
                new HttpEntity<>(authHeaders(interno)), JsonNode.class).getBody().get("id").asText();

        Instant inicio = future();
        JsonNode created = postEvent(interno, eventRequest("Autoria", externoId, inicio, inicio.plus(1, ChronoUnit.HOURS)))
                .getBody();

        assertThat(created.get("createdBy").asText()).isEqualTo(internoId);
    }

    // ---- helpers ----

    protected Instant future() {
        return Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
    }

    protected ResponseEntity<JsonNode> postEvent(String token, Map<String, Object> request) {
        return restTemplate.exchange("/events", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders(token)), JsonNode.class);
    }

    protected Map<String, Object> eventRequest(String titulo, String externalUserId, Instant inicio, Instant fim) {
        Map<String, Object> request = new HashMap<>();
        request.put("titulo", titulo);
        request.put("descricao", "Descrição do evento");
        request.put("dataHoraInicio", inicio.toString());
        request.put("dataHoraFim", fim.toString());
        request.put("externalUserId", externalUserId);
        return request;
    }

    protected String gerenteToken() {
        return login("gerente@unimobili.com", "admin12345");
    }

    protected String login(String email, String senha) {
        ResponseEntity<JsonNode> login = restTemplate.postForEntity(
                "/auth/login", Map.of("email", email, "senha", senha), JsonNode.class);
        return login.getBody().get("accessToken").asText();
    }

    /** Cria usuário via API e devolve o id. Senha padrão "senha12345". */
    protected String createUser(String token, String nome, String email, String role) {
        Map<String, Object> request = Map.of(
                "nome", nome, "email", email, "senha", "senha12345", "role", role);
        return restTemplate.exchange("/users", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders(token)), JsonNode.class).getBody().get("id").asText();
    }

    protected HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
