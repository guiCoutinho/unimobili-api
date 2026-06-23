package com.unimobili.agenda.security;

import com.unimobili.agenda.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActorTest {

    private final UUID me = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();

    @Test
    void gerenteCanModifyAnyEvent() {
        Actor gerente = new Actor(me, Role.GERENTE);
        assertThatCode(() -> gerente.assertCanModify(other)).doesNotThrowAnyException();
    }

    @Test
    void internoCanModifyOwnEventButNotOthers() {
        Actor interno = new Actor(me, Role.INTERNO);
        assertThatCode(() -> interno.assertCanModify(me)).doesNotThrowAnyException();
        assertThatThrownBy(() -> interno.assertCanModify(other))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void externoCanViewOwnAgendaButNotOthers() {
        Actor externo = new Actor(me, Role.EXTERNO);
        assertThatCode(() -> externo.assertCanViewAgendaOf(me)).doesNotThrowAnyException();
        assertThatThrownBy(() -> externo.assertCanViewAgendaOf(other))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void managerCanViewAnyAgenda() {
        Actor gerente = new Actor(me, Role.GERENTE);
        assertThatCode(() -> gerente.assertCanViewAgendaOf(other)).doesNotThrowAnyException();
    }

    @Test
    void externoFilterIsAlwaysForcedToOwnId() {
        Actor externo = new Actor(me, Role.EXTERNO);
        assertThat(externo.effectiveExternalFilter(other)).isEqualTo(me);
        assertThat(externo.effectiveExternalFilter(null)).isEqualTo(me);
    }

    @Test
    void managerFilterIsTheRequestedValue() {
        Actor gerente = new Actor(me, Role.GERENTE);
        assertThat(gerente.effectiveExternalFilter(other)).isEqualTo(other);
        assertThat(gerente.effectiveExternalFilter(null)).isNull();
    }
}
