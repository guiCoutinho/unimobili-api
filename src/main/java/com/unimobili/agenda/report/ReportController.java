package com.unimobili.agenda.report;

import com.unimobili.agenda.event.EventStatus;
import com.unimobili.agenda.event.dto.EventResponse;
import com.unimobili.agenda.report.dto.CancellationReport;
import com.unimobili.agenda.report.dto.EventsByUserReport;
import com.unimobili.agenda.report.dto.OccupancyReport;
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
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/events")
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
    public List<EventsByUserReport> eventsByUser(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant ate,
            @RequestParam(required = false) UUID createdBy,
            @RequestParam(required = false) EventStatus status) {
        return reportService.eventsByUser(de, ate, createdBy, status);
    }

    @GetMapping("/cancellations")
    public CancellationReport cancellations(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant ate,
            @RequestParam(required = false) UUID externalUserId) {
        return reportService.cancellations(de, ate, externalUserId);
    }

    @GetMapping("/occupancy")
    public List<OccupancyReport> occupancy(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant ate) {
        return reportService.occupancy(de, ate);
    }
}
