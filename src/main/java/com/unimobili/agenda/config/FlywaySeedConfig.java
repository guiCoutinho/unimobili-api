package com.unimobili.agenda.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

/**
 * Resolve os placeholders usados pela migration de seed do gerente,
 * calculando o hash BCrypt da senha configurada em tempo de execução.
 */
@Configuration
public class FlywaySeedConfig {

    @Bean
    FlywayConfigurationCustomizer seedGerentePlaceholders(
            PasswordEncoder passwordEncoder,
            @Value("${app.seed.gerente.email}") String email,
            @Value("${app.seed.gerente.password}") String rawPassword) {
        return configuration -> configuration.placeholders(Map.of(
                "gerente_email", email,
                "gerente_password_hash", passwordEncoder.encode(rawPassword)
        ));
    }
}
