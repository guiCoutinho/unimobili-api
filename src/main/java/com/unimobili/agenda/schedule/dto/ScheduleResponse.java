package com.unimobili.agenda.schedule.dto;

import com.unimobili.agenda.event.dto.EventResponse;

import java.util.List;
import java.util.UUID;

public record ScheduleResponse(
        UUID externalUserId,
        String nome,
        String email,
        List<EventResponse> events
) {
}
