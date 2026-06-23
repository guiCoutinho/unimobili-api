package com.unimobili.agenda.security;

import com.unimobili.agenda.user.Role;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adapter fino que constrói o {@link Actor} a partir do JWT no contexto de segurança.
 * É o único ponto que conhece o SecurityContext; a política em si vive no Actor.
 */
@Component
public class CurrentActor {

    public Actor current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return new Actor(
                    UUID.fromString(jwt.getClaimAsString(JwtService.CLAIM_UID)),
                    Role.valueOf(jwt.getClaimAsString(JwtService.CLAIM_ROLE)));
        }
        throw new AccessDeniedException("Usuário não autenticado");
    }
}
