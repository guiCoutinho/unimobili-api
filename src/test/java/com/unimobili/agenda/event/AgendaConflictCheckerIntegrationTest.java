package com.unimobili.agenda.event;

import com.unimobili.agenda.AbstractIntegrationTest;
import com.unimobili.agenda.user.Role;
import com.unimobili.agenda.user.User;
import com.unimobili.agenda.user.UserRepository;
import com.unimobili.agenda.web.error.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgendaConflictCheckerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    AgendaConflictChecker conflictChecker;
    @Autowired
    EventRepository eventRepository;
    @Autowired
    UserRepository userRepository;

    private final Instant base = Instant.now().plus(300, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);

    @Test
    void throwsWhenOverlappingOccupyingEvent() {
        UUID ext = persistExterno("conf.overlap@unimobili.com");
        persistEvent(ext, base, base.plus(1, ChronoUnit.HOURS), EventStatus.AGENDADO);

        assertThatThrownBy(() -> conflictChecker.assertNoConflict(
                ext, base.plus(30, ChronoUnit.MINUTES), base.plus(90, ChronoUnit.MINUTES), null))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void allowsAdjacentEvent() {
        UUID ext = persistExterno("conf.adjacent@unimobili.com");
        persistEvent(ext, base, base.plus(1, ChronoUnit.HOURS), EventStatus.AGENDADO);

        assertThatCode(() -> conflictChecker.assertNoConflict(
                ext, base.plus(1, ChronoUnit.HOURS), base.plus(2, ChronoUnit.HOURS), null))
                .doesNotThrowAnyException();
    }

    @Test
    void cancelledEventFreesTheSlot() {
        UUID ext = persistExterno("conf.cancelled@unimobili.com");
        persistEvent(ext, base, base.plus(1, ChronoUnit.HOURS), EventStatus.CANCELADO);

        assertThatCode(() -> conflictChecker.assertNoConflict(
                ext, base, base.plus(1, ChronoUnit.HOURS), null))
                .doesNotThrowAnyException();
    }

    @Test
    void excludeEventIdIgnoresTheEventItself() {
        UUID ext = persistExterno("conf.exclude@unimobili.com");
        Event existing = persistEvent(ext, base, base.plus(1, ChronoUnit.HOURS), EventStatus.AGENDADO);

        assertThatCode(() -> conflictChecker.assertNoConflict(
                ext, base, base.plus(1, ChronoUnit.HOURS), existing.getId()))
                .doesNotThrowAnyException();
    }

    private UUID persistExterno(String email) {
        User u = new User();
        u.setNome("Externo Conflito");
        u.setEmail(email);
        u.setSenha("irrelevante");
        u.setRole(Role.EXTERNO);
        u.setAtivo(true);
        return userRepository.save(u).getId();
    }

    private Event persistEvent(UUID externalUserId, Instant inicio, Instant fim, EventStatus status) {
        Event e = new Event();
        e.setTitulo("Evento");
        e.setDataHoraInicio(inicio);
        e.setDataHoraFim(fim);
        e.setStatus(status);
        e.setExternalUser(userRepository.findById(externalUserId).orElseThrow());
        return eventRepository.save(e);
    }
}
