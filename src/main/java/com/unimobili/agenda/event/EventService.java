package com.unimobili.agenda.event;

import com.unimobili.agenda.event.dto.CreateEventRequest;
import com.unimobili.agenda.event.dto.EventResponse;
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
    private final Clock clock;

    public EventService(EventRepository eventRepository,
                        UserRepository userRepository,
                        EventMapper eventMapper,
                        Clock clock) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.eventMapper = eventMapper;
        this.clock = clock;
    }

    @Transactional
    public EventResponse create(CreateEventRequest request) {
        if (!request.dataHoraFim().isAfter(request.dataHoraInicio())) {
            throw new BadRequestException("O horário de término deve ser maior que o de início");
        }
        if (request.dataHoraInicio().isBefore(clock.instant())) {
            throw new BadRequestException("Não é possível criar evento no passado");
        }

        User externalUser = userRepository.findById(request.externalUserId())
                .orElseThrow(() -> new BadRequestException(
                        "Funcionário externo não encontrado: " + request.externalUserId()));
        if (externalUser.getRole() != Role.EXTERNO || !externalUser.isAtivo()) {
            throw new BadRequestException("O evento deve ser associado a um funcionário EXTERNO ativo");
        }

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

    @Transactional(readOnly = true)
    public Page<EventResponse> list(Instant de, Instant ate, UUID externalUserId,
                                    UUID createdBy, EventStatus status, Pageable pageable) {
        return eventRepository
                .findAll(EventSpecifications.filter(de, ate, externalUserId, createdBy, status), pageable)
                .map(eventMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public EventResponse getById(UUID id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado: " + id));
        return eventMapper.toResponse(event);
    }
}
