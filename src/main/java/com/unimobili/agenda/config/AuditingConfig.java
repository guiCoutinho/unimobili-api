package com.unimobili.agenda.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

@Configuration
@EnableJpaAuditing
public class AuditingConfig {

    /**
     * Auditor atual = id do usuário autenticado (claim "uid" do JWT).
     */
    @Bean
    AuditorAware<UUID> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null
                    || !authentication.isAuthenticated()
                    || !(authentication.getPrincipal() instanceof Jwt jwt)) {
                return Optional.empty();
            }
            String uid = jwt.getClaimAsString("uid");
            return uid == null ? Optional.empty() : Optional.of(UUID.fromString(uid));
        };
    }
}
