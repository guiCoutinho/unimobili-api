package com.unimobili.agenda.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record UpdateEventRequest(
        @NotBlank String titulo,
        String descricao,
        @NotNull Instant dataHoraInicio,
        @NotNull Instant dataHoraFim,
        @NotNull UUID externalUserId
) {
}
