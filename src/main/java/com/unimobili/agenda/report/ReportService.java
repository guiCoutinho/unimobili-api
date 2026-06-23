package com.unimobili.agenda.report;

import com.unimobili.agenda.event.Event;
import com.unimobili.agenda.event.EventMapper;
import com.unimobili.agenda.event.EventRepository;
import com.unimobili.agenda.event.EventSpecifications;
import com.unimobili.agenda.event.EventStatus;
import com.unimobili.agenda.event.dto.EventResponse;
import com.unimobili.agenda.report.dto.CancellationReport;
import com.unimobili.agenda.report.dto.EventsByUserReport;
import com.unimobili.agenda.report.dto.OccupancyReport;
import com.unimobili.agenda.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;

    public ReportService(EventRepository eventRepository, EventMapper eventMapper) {
        this.eventRepository = eventRepository;
        this.eventMapper = eventMapper;
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> events(Instant de, Instant ate, UUID externalUserId,
                                      UUID createdBy, EventStatus status, Pageable pageable) {
        return eventRepository
                .findAll(EventSpecifications.filter(de, ate, externalUserId, createdBy, status), pageable)
                .map(eventMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<EventsByUserReport> eventsByUser(Instant de, Instant ate, UUID createdBy, EventStatus status) {
        Map<UUID, List<Event>> grouped = eventRepository
                .findAll(EventSpecifications.filter(de, ate, null, createdBy, status))
                .stream()
                .collect(Collectors.groupingBy(e -> e.getExternalUser().getId()));

        return grouped.values().stream()
                .map(list -> {
                    User external = list.get(0).getExternalUser();
                    return new EventsByUserReport(external.getId(), external.getNome(), list.size());
                })
                .sorted(Comparator.comparing(EventsByUserReport::nome))
                .toList();
    }

    @Transactional(readOnly = true)
    public CancellationReport cancellations(Instant de, Instant ate, UUID externalUserId) {
        long total = eventRepository.count(EventSpecifications.filter(de, ate, externalUserId, null, null));
        long cancelados = eventRepository.count(
                EventSpecifications.filter(de, ate, externalUserId, null, EventStatus.CANCELADO));
        long naoCompareceu = eventRepository.count(
                EventSpecifications.filter(de, ate, externalUserId, null, EventStatus.NAO_COMPARECEU));
        double taxa = total == 0 ? 0.0 : (double) (cancelados + naoCompareceu) / total;
        return new CancellationReport(total, cancelados, naoCompareceu, taxa);
    }

    @Transactional(readOnly = true)
    public List<OccupancyReport> occupancy(Instant de, Instant ate) {
        Map<UUID, List<Event>> grouped = eventRepository
                .findAll(EventSpecifications.filter(de, ate, null, null, null))
                .stream()
                .filter(e -> e.getStatus().occupiesSlot())
                .collect(Collectors.groupingBy(e -> e.getExternalUser().getId()));

        return grouped.values().stream()
                .map(list -> {
                    User external = list.get(0).getExternalUser();
                    long minutos = list.stream()
                            .mapToLong(e -> Duration.between(e.getDataHoraInicio(), e.getDataHoraFim()).toMinutes())
                            .sum();
                    return new OccupancyReport(external.getId(), external.getNome(), list.size(), minutos / 60.0);
                })
                .sorted(Comparator.comparing(OccupancyReport::nome))
                .toList();
    }
}
