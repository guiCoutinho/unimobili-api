package com.unimobili.agenda.event;

import com.unimobili.agenda.web.error.ConflictException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Lar único da regra de conflito de agenda: encapsula o predicado de sobreposição
 * e a definição de status que ocupam o slot. Um evento conflita quando, para o mesmo
 * funcionário externo e status ocupante, {@code inicioNovo < fimExistente AND inicioExistente < fimNovo}.
 */
@Component
public class AgendaConflictChecker {

    private final EventRepository eventRepository;

    public AgendaConflictChecker(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    /**
     * Garante que não há sobreposição na agenda do externo no período informado.
     *
     * @param excludeEventId evento a ignorar (o próprio, em edições); {@code null} na criação
     * @throws ConflictException se houver conflito
     */
    public void assertNoConflict(UUID externalUserId, Instant inicio, Instant fim, UUID excludeEventId) {
        boolean conflito = eventRepository.existsConflict(
                externalUserId, EventStatus.occupying(), inicio, fim, excludeEventId);
        if (conflito) {
            throw new ConflictException("Conflito de horário na agenda do funcionário externo");
        }
    }
}
