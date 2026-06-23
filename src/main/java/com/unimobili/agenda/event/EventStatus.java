package com.unimobili.agenda.event;

public enum EventStatus {
    AGENDADO,
    CONFIRMADO,
    REALIZADO,
    CANCELADO,
    NAO_COMPARECEU;

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
