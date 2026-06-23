package com.unimobili.agenda.report;

import com.unimobili.agenda.event.EventStatus;
import com.unimobili.agenda.event.dto.EventResponse;
import com.unimobili.agenda.report.dto.CancellationReport;
import com.unimobili.agenda.report.dto.EventsByUserReport;
import com.unimobili.agenda.report.dto.OccupancyReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/reports")
@Tag(name = "Relatórios", description = "Relatórios gerenciais (somente GERENTE)")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/events")
    @Operation(summary = "Relatório de eventos", description = "Lista de eventos filtrada por período, externo, criador e status.")
    public Page<EventResponse> events(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant ate,
            @RequestParam(required = false) UUID externalUserId,
            @RequestParam(required = false) UUID createdBy,
            @RequestParam(required = false) EventStatus status,
            Pageable pageable) {
        return reportService.events(de, ate, externalUserId, createdBy, status, pageable);
    }

    @GetMapping("/events-by-user")
    @Operation(summary = "Eventos por externo", description = "Contagem de eventos agrupada por funcionário externo.")
    public List<EventsByUserReport> eventsByUser(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant ate,
            @RequestParam(required = false) UUID createdBy,
            @RequestParam(required = false) EventStatus status) {
        return reportService.eventsByUser(de, ate, createdBy, status);
    }

    @GetMapping("/cancellations")
    @Operation(summary = "Cancelamentos", description = "Totais de CANCELADO e NAO_COMPARECEU e a taxa de cancelamento.")
    public CancellationReport cancellations(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant ate,
            @RequestParam(required = false) UUID externalUserId) {
        return reportService.cancellations(de, ate, externalUserId);
    }

    @GetMapping("/occupancy")
    @Operation(summary = "Ocupação", description = "Horas ocupadas por funcionário externo no período (exclui cancelados).")
    public List<OccupancyReport> occupancy(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant ate) {
        return reportService.occupancy(de, ate);
    }
}
