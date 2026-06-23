package com.unimobili.agenda.event;

import com.unimobili.agenda.event.dto.CreateEventRequest;
import com.unimobili.agenda.event.dto.EventResponse;
import com.unimobili.agenda.event.dto.PatchStatusRequest;
import com.unimobili.agenda.event.dto.UpdateEventRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/events")
@Tag(name = "Eventos", description = "Agendamentos: criação, consulta e ciclo de vida")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar evento",
            description = "Cria um evento (INTERNO/GERENTE) para um funcionário externo, validando horário e conflito.")
    public EventResponse create(@Valid @RequestBody CreateEventRequest request) {
        return eventService.create(request);
    }

    @GetMapping
    @Operation(summary = "Listar eventos",
            description = "Lista paginada e filtrável (período, externo, criador, status). EXTERNO vê só os próprios.")
    public Page<EventResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant ate,
            @RequestParam(required = false) UUID externalUserId,
            @RequestParam(required = false) UUID createdBy,
            @RequestParam(required = false) EventStatus status,
            Pageable pageable) {
        return eventService.list(de, ate, externalUserId, createdBy, status, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhar evento", description = "Retorna um evento pelo id.")
    public EventResponse get(@PathVariable UUID id) {
        return eventService.getById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar evento",
            description = "Atualiza um evento (revalida horário/conflito). INTERNO só altera os próprios.")
    public EventResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateEventRequest request) {
        return eventService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Mudar status",
            description = "Aplica uma transição de status válida (máquina de estados).")
    public EventResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody PatchStatusRequest request) {
        return eventService.changeStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancelar evento", description = "Soft delete: muda o status para CANCELADO, liberando o horário.")
    public void cancel(@PathVariable UUID id) {
        eventService.cancel(id);
    }
}
