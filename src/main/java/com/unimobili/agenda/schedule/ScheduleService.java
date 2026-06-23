package com.unimobili.agenda.schedule;

import com.unimobili.agenda.event.EventMapper;
import com.unimobili.agenda.event.EventRepository;
import com.unimobili.agenda.event.EventSpecifications;
import com.unimobili.agenda.event.dto.EventResponse;
import com.unimobili.agenda.schedule.dto.ScheduleResponse;
import com.unimobili.agenda.security.CurrentActor;
import com.unimobili.agenda.user.Role;
import com.unimobili.agenda.user.User;
import com.unimobili.agenda.user.UserRepository;
import com.unimobili.agenda.web.error.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ScheduleService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final CurrentActor currentActor;

    public ScheduleService(UserRepository userRepository,
                           EventRepository eventRepository,
                           EventMapper eventMapper,
                           CurrentActor currentActor) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.eventMapper = eventMapper;
        this.currentActor = currentActor;
    }

    @Transactional(readOnly = true)
    public Page<ScheduleResponse> listSchedules(Instant de, Instant ate, Pageable pageable) {
        // EXTERNO vê apenas a própria agenda; demais papéis veem todas.
        UUID ownAgendaOnly = currentActor.current().effectiveExternalFilter(null);
        if (ownAgendaOnly != null) {
            User self = userRepository.findById(ownAgendaOnly)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
            return new PageImpl<>(List.of(toSchedule(self, de, ate)), pageable, 1);
        }
        return userRepository.findByRoleAndAtivoTrue(Role.EXTERNO, pageable)
                .map(external -> toSchedule(external, de, ate));
    }

    @Transactional(readOnly = true)
    public ScheduleResponse getSchedule(UUID externalUserId, Instant de, Instant ate) {
        currentActor.current().assertCanViewAgendaOf(externalUserId);

        User external = userRepository.findById(externalUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Agenda não encontrada: " + externalUserId));
        if (external.getRole() != Role.EXTERNO) {
            throw new ResourceNotFoundException("Agenda não encontrada: " + externalUserId);
        }
        return toSchedule(external, de, ate);
    }

    private ScheduleResponse toSchedule(User external, Instant de, Instant ate) {
        List<EventResponse> events = eventRepository
                .findAll(EventSpecifications.filter(de, ate, external.getId(), null, null),
                        Sort.by("dataHoraInicio"))
                .stream()
                .map(eventMapper::toResponse)
                .toList();
        return new ScheduleResponse(external.getId(), external.getNome(), external.getEmail(), events);
    }
}
