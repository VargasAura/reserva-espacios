package co.edu.uis.reservas.domain;

import java.util.List;

/** Estados del diagrama de estados del SRS. */
public enum EstadoReserva {
    RESERVADA, CONFIRMADA, EN_USO, FINALIZADA, CANCELADA, NO_SHOW;

    /** Estados que bloquean el horario del espacio (REQ-28). */
    public static final List<EstadoReserva> ACTIVAS = List.of(RESERVADA, CONFIRMADA, EN_USO);
}
