package com.unimobili.agenda.auth.dto;

import com.unimobili.agenda.user.Role;

import java.util.UUID;

public record MeResponse(
        UUID id,
        String nome,
        String email,
        Role role
) {
}
