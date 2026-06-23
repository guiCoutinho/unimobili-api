package com.unimobili.agenda.schedule;

import com.unimobili.agenda.schedule.dto.ScheduleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/schedules")
@Tag(name = "Agendas", description = "Visão das agendas dos funcionários externos")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping
    @Operation(summary = "Listar agendas",
            description = "Agendas dos externos com seus eventos (filtro de período). EXTERNO vê só a própria.")
    public Page<ScheduleResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant ate,
            Pageable pageable) {
        return scheduleService.listSchedules(de, ate, pageable);
    }

    @GetMapping("/{externalUserId}")
    @Operation(summary = "Agenda de um externo",
            description = "Eventos do funcionário externo no período. EXTERNO só acessa a própria.")
    public ScheduleResponse get(
            @PathVariable UUID externalUserId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant ate) {
        return scheduleService.getSchedule(externalUserId, de, ate);
    }
}
