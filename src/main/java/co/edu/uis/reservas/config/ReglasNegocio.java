package co.edu.uis.reservas.config;

import java.time.Duration;

/**
 * Reglas de negocio del SRS (sección 5.5). Valores iniciales razonables; si el grupo define otros, se cambian SOLO aquí.
 * Todos los módulos deben usar estas constantes (nunca números sueltos en el código).
 */
public final class ReglasNegocio {

    private ReglasNegocio() {}

    /** REQ-16: duración mínima y máxima de una reserva. */
    public static final Duration DURACION_MINIMA = Duration.ofMinutes(30);
    public static final Duration DURACION_MAXIMA = Duration.ofHours(4);

    /** REQ-18: tiempo para confirmar una reserva en estado RESERVADA antes de liberarla. */
    public static final Duration PLAZO_CONFIRMACION = Duration.ofMinutes(30);

    /** REQ-20: anticipación mínima para cancelar una reserva CONFIRMADA. */
    public static final Duration ANTICIPACION_CANCELACION = Duration.ofHours(2);

    /** REQ-25: tolerancia después de la hora de inicio antes de marcar NO_SHOW. */
    public static final Duration TOLERANCIA_NO_SHOW = Duration.ofMinutes(15);

    /** REQ-34: anticipación del recordatorio. */
    public static final Duration ANTICIPACION_RECORDATORIO = Duration.ofHours(24);

    /** REQ-39: reintentos de envío de notificaciones. */
    public static final int REINTENTOS_NOTIFICACION = 3;
}
