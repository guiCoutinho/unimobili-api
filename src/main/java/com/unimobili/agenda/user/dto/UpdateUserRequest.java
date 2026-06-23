package com.unimobili.agenda.user.dto;

import com.unimobili.agenda.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRequest(
        @NotBlank String nome,
        @NotBlank @Email String email,
        String telefone,
        String cargo,
        @NotNull Role role,
        @NotNull Boolean ativo
) {
}
