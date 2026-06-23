package com.unimobili.agenda.event.dto;

import com.unimobili.agenda.event.EventStatus;
import jakarta.validation.constraints.NotNull;

public record PatchStatusRequest(
        @NotNull EventStatus status
) {
}
