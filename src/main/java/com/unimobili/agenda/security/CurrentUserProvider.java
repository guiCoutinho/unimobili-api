package com.unimobili.agenda.security;

import com.unimobili.agenda.user.Role;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Acesso ao usuário autenticado (id e papel) a partir do JWT no contexto de segurança.
 */
@Component
public class CurrentUserProvider {

    public UUID currentUserId() {
        return UUID.fromString(jwt().getClaimAsString(JwtService.CLAIM_UID));
    }

    public Role currentRole() {
        return Role.valueOf(jwt().getClaimAsString(JwtService.CLAIM_ROLE));
    }

    private Jwt jwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt;
        }
        throw new AccessDeniedException("Usuário não autenticado");
    }
}
