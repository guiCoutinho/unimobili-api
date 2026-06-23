package com.unimobili.agenda.report;

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
import static org.assertj.core.api.Assertions.within;

class ReportIntegrationTest extends AbstractIntegrationTest {

    @Test
    void managerGetsEventsReport() {
        String gerente = gerenteToken();
        String externoId = createUser(gerente, "Ext Rel", "ext.rel@unimobili.com", "EXTERNO");
        Instant inicio = future();
        postEvent(gerente, eventRequest("Relatorio", externoId, inicio, inicio.plus(1, ChronoUnit.HOURS)));

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/reports/events?page=0&size=10", HttpMethod.GET,
                new HttpEntity<>(authHeaders(gerente)), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("content").isArray()).isTrue();
        assertThat(response.getBody().get("page").get("totalElements").asInt()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void nonManagerCannotAccessReports() {
        String gerente = gerenteToken();
        createUser(gerente, "Int Rel", "int.rel@unimobili.com", "INTERNO");
        String interno = login("int.rel@unimobili.com", "senha12345");

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/reports/events", HttpMethod.GET, new HttpEntity<>(authHeaders(interno)), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void eventsByUserCountsPerExternal() {
        String gerente = gerenteToken();
        String extAId = createUser(gerente, "Ext RA", "ext.ra@unimobili.com", "EXTERNO");
        String extBId = createUser(gerente, "Ext RB", "ext.rb@unimobili.com", "EXTERNO");
        Instant base = Instant.now().plus(100, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        postEvent(gerente, eventRequest("A1", extAId, base, base.plus(1, ChronoUnit.HOURS)));
        postEvent(gerente, eventRequest("A2", extAId, base.plus(2, ChronoUnit.HOURS), base.plus(3, ChronoUnit.HOURS)));
        postEvent(gerente, eventRequest("B1", extBId, base, base.plus(1, ChronoUnit.HOURS)));

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/reports/events-by-user?de=" + base.minus(1, ChronoUnit.HOURS) + "&ate=" + base.plus(2, ChronoUnit.DAYS),
                HttpMethod.GET, new HttpEntity<>(authHeaders(gerente)), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(findByExternal(response.getBody(), extAId).get("total").asInt()).isEqualTo(2);
        assertThat(findByExternal(response.getBody(), extBId).get("total").asInt()).isEqualTo(1);
    }

    @Test
    void cancellationsReportComputesTotalsAndRate() {
        String gerente = gerenteToken();
        String extId = createUser(gerente, "Ext Canc", "ext.canc.rel@unimobili.com", "EXTERNO");
        Instant base = Instant.now().plus(150, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        String agendado = postEvent(gerente, eventRequest("Fica", extId, base, base.plus(1, ChronoUnit.HOURS)))
                .getBody().get("id").asText();
        String cancelar = postEvent(gerente, eventRequest("Cancela", extId, base.plus(2, ChronoUnit.HOURS), base.plus(3, ChronoUnit.HOURS)))
                .getBody().get("id").asText();
        String faltou = postEvent(gerente, eventRequest("Faltou", extId, base.plus(4, ChronoUnit.HOURS), base.plus(5, ChronoUnit.HOURS)))
                .getBody().get("id").asText();

        deleteEvent(gerente, cancelar);                 // -> CANCELADO
        patchStatus(gerente, faltou, "CONFIRMADO");
        patchStatus(gerente, faltou, "NAO_COMPARECEU"); // -> NAO_COMPARECEU

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/reports/cancellations?externalUserId=" + extId, HttpMethod.GET,
                new HttpEntity<>(authHeaders(gerente)), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("totalEventos").asInt()).isEqualTo(3);
        assertThat(response.getBody().get("cancelados").asInt()).isEqualTo(1);
        assertThat(response.getBody().get("naoCompareceu").asInt()).isEqualTo(1);
        assertThat(response.getBody().get("taxaCancelamento").asDouble()).isCloseTo(2.0 / 3.0, within(0.0001));
        assertThat(agendado).isNotBlank();
    }

    @Test
    void occupancyReportSumsHoursExcludingCancelled() {
        String gerente = gerenteToken();
        String extId = createUser(gerente, "Ext Ocup", "ext.ocup@unimobili.com", "EXTERNO");
        Instant base = Instant.now().plus(200, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        postEvent(gerente, eventRequest("1h", extId, base, base.plus(1, ChronoUnit.HOURS)));
        postEvent(gerente, eventRequest("2h", extId, base.plus(2, ChronoUnit.HOURS), base.plus(4, ChronoUnit.HOURS)));
        String cancelado = postEvent(gerente, eventRequest("Cancelado", extId, base.plus(5, ChronoUnit.HOURS), base.plus(6, ChronoUnit.HOURS)))
                .getBody().get("id").asText();
        deleteEvent(gerente, cancelado); // excluido da ocupacao

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/reports/occupancy?de=" + base.minus(1, ChronoUnit.HOURS) + "&ate=" + base.plus(1, ChronoUnit.DAYS),
                HttpMethod.GET, new HttpEntity<>(authHeaders(gerente)), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode entry = findByExternal(response.getBody(), extId);
        assertThat(entry.get("totalEventos").asInt()).isEqualTo(2);
        assertThat(entry.get("horasOcupadas").asDouble()).isCloseTo(3.0, within(0.0001));
    }

    // ---- helpers ----

    protected JsonNode findByExternal(JsonNode array, String externalUserId) {
        for (JsonNode node : array) {
            if (node.get("externalUserId").asText().equals(externalUserId)) {
                return node;
            }
        }
        throw new AssertionError("Externo não encontrado no relatório: " + externalUserId);
    }

    protected ResponseEntity<JsonNode> patchStatus(String token, String id, String status) {
        return restTemplate.exchange("/events/" + id + "/status", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("status", status), authHeaders(token)), JsonNode.class);
    }

    protected ResponseEntity<Void> deleteEvent(String token, String id) {
        return restTemplate.exchange("/events/" + id, HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(token)), Void.class);
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
