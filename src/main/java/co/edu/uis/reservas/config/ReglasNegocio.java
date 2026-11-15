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

    /** Las horas de inicio y fin solo pueden caer en punto o y media (1:00, 1:30, 2:00, 2:30...). */
    public static final int INTERVALO_HORARIO_MINUTOS = 30;

    /**
     * Recordatorio por mensaje de texto: si pasado este tiempo desde la hora de inicio el espacio todavía
     * no se marcó como ocupado (EN_USO), se le recuerda a la persona su reserva.
     */
    public static final Duration RECORDATORIO_SIN_OCUPAR = Duration.ofMinutes(15);

    /**
     * REQ-25: tolerancia después de la hora de inicio antes de marcar NO_SHOW.
     * Debe ser MAYOR que RECORDATORIO_SIN_OCUPAR, para que el recordatorio llegue antes de la inasistencia.
     */
    public static final Duration TOLERANCIA_NO_SHOW = Duration.ofMinutes(30);

    /** REQ-34: anticipación del recordatorio. */
    public static final Duration ANTICIPACION_RECORDATORIO = Duration.ofHours(24);

    /** REQ-39: reintentos de envío de notificaciones. */
    public static final int REINTENTOS_NOTIFICACION = 3;
}
