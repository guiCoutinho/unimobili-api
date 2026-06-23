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

class EventLifecycleIntegrationTest extends AbstractIntegrationTest {

    @Test
    void creatorUpdatesOwnEvent() {
        String gerente = gerenteToken();
        String externoId = createUser(gerente, "Ext Ciclo", "ext.ciclo@unimobili.com", "EXTERNO");
        createUser(gerente, "Int Ciclo", "int.ciclo@unimobili.com", "INTERNO");
        String interno = login("int.ciclo@unimobili.com", "senha12345");

        Instant inicio = future();
        Instant fim = inicio.plus(1, ChronoUnit.HOURS);
        String id = postEvent(interno, eventRequest("Original", externoId, inicio, fim)).getBody().get("id").asText();

        Map<String, Object> update = eventRequest("Atualizado", externoId, inicio, fim);
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/events/" + id, HttpMethod.PUT, new HttpEntity<>(update, authHeaders(interno)), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("titulo").asText()).isEqualTo("Atualizado");
    }

    @Test
    void validStatusTransitionIsAccepted() {
        String gerente = gerenteToken();
        String externoId = createUser(gerente, "Ext Trans", "ext.trans@unimobili.com", "EXTERNO");
        Instant inicio = future();
        String id = postEvent(gerente, eventRequest("Trans", externoId, inicio, inicio.plus(1, ChronoUnit.HOURS)))
                .getBody().get("id").asText();

        ResponseEntity<JsonNode> response = patchStatus(gerente, id, "CONFIRMADO");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status").asText()).isEqualTo("CONFIRMADO");
    }

    @Test
    void invalidStatusTransitionReturns409() {
        String gerente = gerenteToken();
        String externoId = createUser(gerente, "Ext TransInv", "ext.transinv@unimobili.com", "EXTERNO");
        Instant inicio = future();
        String id = postEvent(gerente, eventRequest("TransInv", externoId, inicio, inicio.plus(1, ChronoUnit.HOURS)))
                .getBody().get("id").asText();

        // AGENDADO -> REALIZADO não é permitido
        ResponseEntity<JsonNode> response = patchStatus(gerente, id, "REALIZADO");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void deleteSoftCancelsAndFreesSlot() {
        String gerente = gerenteToken();
        String externoId = createUser(gerente, "Ext Cancel", "ext.cancel@unimobili.com", "EXTERNO");
        Instant inicio = future();
        Instant fim = inicio.plus(1, ChronoUnit.HOURS);
        String id = postEvent(gerente, eventRequest("AserCancelado", externoId, inicio, fim))
                .getBody().get("id").asText();

        ResponseEntity<Void> delete = deleteEvent(gerente, id);
        assertThat(delete.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // soft delete: continua existindo, agora CANCELADO
        assertThat(getEvent(gerente, id).getBody().get("status").asText()).isEqualTo("CANCELADO");

        // slot liberado: um evento sobreposto agora é aceito
        ResponseEntity<JsonNode> reuse = postEvent(gerente,
                eventRequest("Reaproveita", externoId, inicio, fim));
        assertThat(reuse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void internoCannotModifyEventCreatedByAnother() {
        String gerente = gerenteToken();
        String externoId = createUser(gerente, "Ext Prop", "ext.prop@unimobili.com", "EXTERNO");
        createUser(gerente, "Int Dono", "int.dono@unimobili.com", "INTERNO");
        createUser(gerente, "Int Outro", "int.outro@unimobili.com", "INTERNO");
        String dono = login("int.dono@unimobili.com", "senha12345");
        String outro = login("int.outro@unimobili.com", "senha12345");

        Instant inicio = future();
        Instant fim = inicio.plus(1, ChronoUnit.HOURS);
        String id = postEvent(dono, eventRequest("DoEvento", externoId, inicio, fim)).getBody().get("id").asText();

        // outro INTERNO -> 403
        assertThat(putEvent(outro, id, eventRequest("Hack", externoId, inicio, fim)).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        // GERENTE pode alterar qualquer evento -> 200
        assertThat(putEvent(gerente, id, eventRequest("PeloGerente", externoId, inicio, fim)).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void externoCannotChangeStatus() {
        String gerente = gerenteToken();
        String externoId = createUser(gerente, "Ext SemPoder", "ext.sempoder@unimobili.com", "EXTERNO");
        String externoToken = login("ext.sempoder@unimobili.com", "senha12345");
        Instant inicio = future();
        String id = postEvent(gerente, eventRequest("DoExterno", externoId, inicio, inicio.plus(1, ChronoUnit.HOURS)))
                .getBody().get("id").asText();

        assertThat(patchStatus(externoToken, id, "CONFIRMADO").getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(deleteEvent(externoToken, id).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void editingTerminalEventIsBlocked() {
        String gerente = gerenteToken();
        String externoId = createUser(gerente, "Ext Terminal", "ext.terminal@unimobili.com", "EXTERNO");
        Instant inicio = future();
        Instant fim = inicio.plus(1, ChronoUnit.HOURS);
        String id = postEvent(gerente, eventRequest("Terminal", externoId, inicio, fim)).getBody().get("id").asText();
        deleteEvent(gerente, id); // -> CANCELADO (terminal)

        ResponseEntity<JsonNode> response = putEvent(gerente, id, eventRequest("Tentativa", externoId, inicio, fim));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void updateWithPastStartReturns400() {
        String gerente = gerenteToken();
        String externoId = createUser(gerente, "Ext PassEdit", "ext.passedit@unimobili.com", "EXTERNO");
        Instant inicio = future();
        Instant fim = inicio.plus(1, ChronoUnit.HOURS);
        String id = postEvent(gerente, eventRequest("Futuro", externoId, inicio, fim)).getBody().get("id").asText();

        Instant passado = Instant.now().minus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        ResponseEntity<JsonNode> response = putEvent(gerente, id,
                eventRequest("ParaPassado", externoId, passado, passado.plus(1, ChronoUnit.HOURS)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ---- helpers ----

    protected ResponseEntity<JsonNode> putEvent(String token, String id, Map<String, Object> request) {
        return restTemplate.exchange("/events/" + id, HttpMethod.PUT,
                new HttpEntity<>(request, authHeaders(token)), JsonNode.class);
    }

    protected ResponseEntity<JsonNode> patchStatus(String token, String id, String status) {
        return restTemplate.exchange("/events/" + id + "/status", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("status", status), authHeaders(token)), JsonNode.class);
    }

    protected ResponseEntity<Void> deleteEvent(String token, String id) {
        return restTemplate.exchange("/events/" + id, HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(token)), Void.class);
    }

    protected ResponseEntity<JsonNode> getEvent(String token, String id) {
        return restTemplate.exchange("/events/" + id, HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)), JsonNode.class);
    }

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
