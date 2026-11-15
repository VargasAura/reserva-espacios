package co.edu.uis.reservas.config;

import java.time.LocalDateTime;

/** Regla de horas de una reserva: solo en punto o y media (1:00, 1:30, 2:00...). La lista que ve la persona está en hora12.js. */
public final class HorasReserva {

    private HorasReserva() {}

    /** true si la hora cae exactamente en un múltiplo del intervalo (en punto o y media) y sin segundos. */
    public static boolean esValida(LocalDateTime momento) {
        return momento.getSecond() == 0 && momento.getNano() == 0
                && momento.getMinute() % ReglasNegocio.INTERVALO_HORARIO_MINUTOS == 0;
    }
}
