package com.unimobili.agenda.schedule;

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

class ScheduleIntegrationTest extends AbstractIntegrationTest {

    @Test
    void managerGetsExternalScheduleWithEvents() {
        String gerente = gerenteToken();
        String externoId = createUser(gerente, "Ext Agenda", "ext.agenda@unimobili.com", "EXTERNO");
        Instant inicio = future();
        postEvent(gerente, eventRequest("Compromisso", externoId, inicio, inicio.plus(1, ChronoUnit.HOURS)));

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/schedules/" + externoId, HttpMethod.GET,
                new HttpEntity<>(authHeaders(gerente)), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("externalUserId").asText()).isEqualTo(externoId);
        assertThat(response.getBody().get("events").isArray()).isTrue();
        assertThat(response.getBody().get("events").size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void managerListsSchedulesPaginated() {
        String gerente = gerenteToken();
        createUser(gerente, "Ext ListaAg", "ext.listaag@unimobili.com", "EXTERNO");

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/schedules?page=0&size=5", HttpMethod.GET, new HttpEntity<>(authHeaders(gerente)), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("content").isArray()).isTrue();
        assertThat(response.getBody().get("page").get("totalElements").asInt()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void externoSeesOnlyOwnScheduleInList() {
        String gerente = gerenteToken();
        String externoId = createUser(gerente, "Ext Sozinho", "ext.sozinho@unimobili.com", "EXTERNO");
        createUser(gerente, "Ext Outro2", "ext.outro2@unimobili.com", "EXTERNO");
        String externo = login("ext.sozinho@unimobili.com", "senha12345");

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/schedules", HttpMethod.GET, new HttpEntity<>(authHeaders(externo)), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("content").size()).isEqualTo(1);
        assertThat(response.getBody().get("content").get(0).get("externalUserId").asText()).isEqualTo(externoId);
    }

    @Test
    void externoCannotAccessAnotherSchedule() {
        String gerente = gerenteToken();
        String externoAId = createUser(gerente, "Ext A", "ext.a.sched@unimobili.com", "EXTERNO");
        String externoBId = createUser(gerente, "Ext B", "ext.b.sched@unimobili.com", "EXTERNO");
        String externoA = login("ext.a.sched@unimobili.com", "senha12345");

        assertThat(restTemplate.exchange("/schedules/" + externoBId, HttpMethod.GET,
                new HttpEntity<>(authHeaders(externoA)), JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(restTemplate.exchange("/schedules/" + externoAId, HttpMethod.GET,
                new HttpEntity<>(authHeaders(externoA)), JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void unknownExternalScheduleReturns404() {
        String gerente = gerenteToken();
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/schedules/" + java.util.UUID.randomUUID(), HttpMethod.GET,
                new HttpEntity<>(authHeaders(gerente)), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void periodFilterExcludesOutOfRangeEvents() {
        String gerente = gerenteToken();
        String externoId = createUser(gerente, "Ext Periodo", "ext.periodo@unimobili.com", "EXTERNO");
        Instant dentro = future();
        Instant fora = future().plus(10, ChronoUnit.DAYS);
        postEvent(gerente, eventRequest("Dentro", externoId, dentro, dentro.plus(1, ChronoUnit.HOURS)));
        postEvent(gerente, eventRequest("Fora", externoId, fora, fora.plus(1, ChronoUnit.HOURS)));

        Instant de = dentro.minus(1, ChronoUnit.HOURS);
        Instant ate = dentro.plus(2, ChronoUnit.HOURS);
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/schedules/" + externoId + "?de=" + de + "&ate=" + ate, HttpMethod.GET,
                new HttpEntity<>(authHeaders(gerente)), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("events").size()).isEqualTo(1);
        assertThat(response.getBody().get("events").get(0).get("titulo").asText()).isEqualTo("Dentro");
    }

    @Test
    void externoListingEventsSeesOnlyOwn() {
        String gerente = gerenteToken();
        String meuId = createUser(gerente, "Ext Meu", "ext.meu@unimobili.com", "EXTERNO");
        String outroId = createUser(gerente, "Ext Alheio", "ext.alheio@unimobili.com", "EXTERNO");
        Instant inicio = future();
        postEvent(gerente, eventRequest("Meu", meuId, inicio, inicio.plus(1, ChronoUnit.HOURS)));
        postEvent(gerente, eventRequest("Alheio", outroId, inicio, inicio.plus(1, ChronoUnit.HOURS)));
        String externo = login("ext.meu@unimobili.com", "senha12345");

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/events?size=100", HttpMethod.GET, new HttpEntity<>(authHeaders(externo)), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode content = response.getBody().get("content");
        assertThat(content.size()).isGreaterThanOrEqualTo(1);
        content.forEach(e -> assertThat(e.get("externalUserId").asText()).isEqualTo(meuId));
    }

    @Test
    void externoCannotGetAnothersEvent() {
        String gerente = gerenteToken();
        createUser(gerente, "Ext Espia", "ext.espia@unimobili.com", "EXTERNO");
        String alvoId = createUser(gerente, "Ext Alvo", "ext.alvoevt@unimobili.com", "EXTERNO");
        Instant inicio = future();
        String eventoAlheio = postEvent(gerente, eventRequest("Privado", alvoId, inicio, inicio.plus(1, ChronoUnit.HOURS)))
                .getBody().get("id").asText();
        String espia = login("ext.espia@unimobili.com", "senha12345");

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/events/" + eventoAlheio, HttpMethod.GET, new HttpEntity<>(authHeaders(espia)), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
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
