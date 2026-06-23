package com.unimobili.agenda.report.dto;

public record CancellationReport(
        long totalEventos,
        long cancelados,
        long naoCompareceu,
        double taxaCancelamento
) {
}
