package com.unimobili.agenda.user.dto;

import com.unimobili.agenda.user.Role;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String nome,
        String email,
        String telefone,
        String cargo,
        Role role,
        boolean ativo,
        Instant createdAt,
        Instant updatedAt,
        UUID createdBy,
        UUID updatedBy
) {
}
