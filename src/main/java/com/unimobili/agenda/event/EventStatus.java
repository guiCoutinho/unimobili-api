package com.unimobili.agenda.event;

import java.util.EnumSet;
import java.util.Set;

public enum EventStatus {
    AGENDADO,
    CONFIRMADO,
    REALIZADO,
    CANCELADO,
    NAO_COMPARECEU;

    /** Conjunto de status que ocupam a agenda — fonte única, derivada de occupiesSlot(). */
    public static Set<EventStatus> occupying() {
        EnumSet<EventStatus> occupying = EnumSet.noneOf(EventStatus.class);
        for (EventStatus status : values()) {
            if (status.occupiesSlot()) {
                occupying.add(status);
            }
        }
        return occupying;
    }

    /** Status terminais não admitem novas transições nem edição. */
    public boolean isTerminal() {
        return this == REALIZADO || this == CANCELADO || this == NAO_COMPARECEU;
    }

    /** Ocupa a agenda (não foi cancelado nem marcado como não comparecimento). */
    public boolean occupiesSlot() {
        return this != CANCELADO && this != NAO_COMPARECEU;
    }

    public boolean canTransitionTo(EventStatus target) {
        return switch (this) {
            case AGENDADO -> target == CONFIRMADO || target == CANCELADO;
            case CONFIRMADO -> target == REALIZADO || target == CANCELADO || target == NAO_COMPARECEU;
            default -> false;
        };
    }
}
