package com.unimobili.agenda.security;

import com.unimobili.agenda.user.Role;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

/**
 * O usuário autenticado que executa uma ação, como objeto de domínio.
 * Concentra as decisões de autorização que a rota não expressa: propriedade de
 * evento (INTERNO) e visibilidade por linha (EXTERNO). Value object puro — sem
 * Spring/JPA — testável pela interface.
 */
public record Actor(UUID id, Role role) {

    /** INTERNO só altera o que criou; GERENTE altera qualquer um. */
    public void assertCanModify(UUID createdBy) {
        if (role == Role.INTERNO && !id.equals(createdBy)) {
            throw new AccessDeniedException("Você só pode alterar eventos criados por você");
        }
    }

    /** EXTERNO só acessa a própria agenda; demais papéis acessam qualquer uma. */
    public void assertCanViewAgendaOf(UUID externalUserId) {
        if (role == Role.EXTERNO && !id.equals(externalUserId)) {
            throw new AccessDeniedException("Você só pode acessar a própria agenda");
        }
    }

    /** Filtro de externo efetivo: EXTERNO é forçado à própria agenda; demais usam o solicitado. */
    public UUID effectiveExternalFilter(UUID requested) {
        return role == Role.EXTERNO ? id : requested;
    }
}
