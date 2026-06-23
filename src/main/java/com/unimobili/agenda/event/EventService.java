package com.unimobili.agenda.event;

import com.unimobili.agenda.event.dto.CreateEventRequest;
import com.unimobili.agenda.event.dto.EventResponse;
import com.unimobili.agenda.event.dto.UpdateEventRequest;
import com.unimobili.agenda.security.CurrentActor;
import com.unimobili.agenda.user.Role;
import com.unimobili.agenda.user.User;
import com.unimobili.agenda.user.UserRepository;
import com.unimobili.agenda.web.error.BadRequestException;
import com.unimobili.agenda.web.error.ConflictException;
import com.unimobili.agenda.web.error.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
public class EventService {

    /** Status que ocupam a agenda (CANCELADO e NAO_COMPARECEU liberam o horário). */
    static final Set<EventStatus> OCCUPYING =
            EnumSet.of(EventStatus.AGENDADO, EventStatus.CONFIRMADO, EventStatus.REALIZADO);

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;
    private final CurrentActor currentActor;
    private final Clock clock;

    public EventService(EventRepository eventRepository,
                        UserRepository userRepository,
                        EventMapper eventMapper,
                        CurrentActor currentActor,
                        Clock clock) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.eventMapper = eventMapper;
        this.currentActor = currentActor;
        this.clock = clock;
    }

    @Transactional
    public EventResponse create(CreateEventRequest request) {
        validatePeriod(request.dataHoraInicio(), request.dataHoraFim());
        User externalUser = loadExternalUser(request.externalUserId());

        if (eventRepository.existsConflict(externalUser.getId(), OCCUPYING,
                request.dataHoraInicio(), request.dataHoraFim())) {
            throw new ConflictException("Conflito de horário na agenda do funcionário externo");
        }

        Event event = new Event();
        event.setTitulo(request.titulo());
        event.setDescricao(request.descricao());
        event.setDataHoraInicio(request.dataHoraInicio());
        event.setDataHoraFim(request.dataHoraFim());
        event.setStatus(EventStatus.AGENDADO);
        event.setExternalUser(externalUser);

        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Transactional
    public EventResponse update(UUID id, UpdateEventRequest request) {
        Event event = findOrThrow(id);
        assertCanModify(event);
        if (event.getStatus().isTerminal()) {
            throw new ConflictException("Evento em status terminal não pode ser editado");
        }

        validatePeriod(request.dataHoraInicio(), request.dataHoraFim());
        User externalUser = loadExternalUser(request.externalUserId());

        if (eventRepository.existsConflictExcluding(id, externalUser.getId(), OCCUPYING,
                request.dataHoraInicio(), request.dataHoraFim())) {
            throw new ConflictException("Conflito de horário na agenda do funcionário externo");
        }

        event.setTitulo(request.titulo());
        event.setDescricao(request.descricao());
        event.setDataHoraInicio(request.dataHoraInicio());
        event.setDataHoraFim(request.dataHoraFim());
        event.setExternalUser(externalUser);

        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Transactional
    public EventResponse changeStatus(UUID id, EventStatus target) {
        Event event = findOrThrow(id);
        assertCanModify(event);
        if (!event.getStatus().canTransitionTo(target)) {
            throw new ConflictException(
                    "Transição de status inválida: " + event.getStatus() + " -> " + target);
        }
        event.setStatus(target);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Transactional
    public void cancel(UUID id) {
        Event event = findOrThrow(id);
        assertCanModify(event);
        if (!event.getStatus().canTransitionTo(EventStatus.CANCELADO)) {
            throw new ConflictException(
                    "Evento em status terminal não pode ser cancelado: " + event.getStatus());
        }
        event.setStatus(EventStatus.CANCELADO);
        eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> list(Instant de, Instant ate, UUID externalUserId,
                                    UUID createdBy, EventStatus status, Pageable pageable) {
        // EXTERNO só enxerga a própria agenda.
        UUID effectiveExternalUserId = currentActor.current().effectiveExternalFilter(externalUserId);
        return eventRepository
                .findAll(EventSpecifications.filter(de, ate, effectiveExternalUserId, createdBy, status), pageable)
                .map(eventMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public EventResponse getById(UUID id) {
        Event event = findOrThrow(id);
        currentActor.current().assertCanViewAgendaOf(event.getExternalUser().getId());
        return eventMapper.toResponse(event);
    }

    // ---- regras compartilhadas ----

    private void validatePeriod(Instant inicio, Instant fim) {
        if (!fim.isAfter(inicio)) {
            throw new BadRequestException("O horário de término deve ser maior que o de início");
        }
        if (inicio.isBefore(clock.instant())) {
            throw new BadRequestException("Não é possível agendar evento no passado");
        }
    }

    private User loadExternalUser(UUID externalUserId) {
        User externalUser = userRepository.findById(externalUserId)
                .orElseThrow(() -> new BadRequestException(
                        "Funcionário externo não encontrado: " + externalUserId));
        if (externalUser.getRole() != Role.EXTERNO || !externalUser.isAtivo()) {
            throw new BadRequestException("O evento deve ser associado a um funcionário EXTERNO ativo");
        }
        return externalUser;
    }

    /** Delega ao Actor: INTERNO só altera eventos que criou; GERENTE altera qualquer um. */
    private void assertCanModify(Event event) {
        currentActor.current().assertCanModify(event.getCreatedBy());
    }

    private Event findOrThrow(UUID id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado: " + id));
    }
}
