package com.unimobili.agenda.event.dto;

import com.unimobili.agenda.event.EventStatus;

import java.time.Instant;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String titulo,
        String descricao,
        Instant dataHoraInicio,
        Instant dataHoraFim,
        EventStatus status,
        UUID externalUserId,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
