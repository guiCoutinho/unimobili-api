package com.unimobili.agenda.report.dto;

import java.util.UUID;

public record EventsByUserReport(
        UUID externalUserId,
        String nome,
        long total
) {
}
