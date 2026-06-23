package com.unimobili.agenda.report.dto;

import java.util.UUID;

public record OccupancyReport(
        UUID externalUserId,
        String nome,
        long totalEventos,
        double horasOcupadas
) {
}
