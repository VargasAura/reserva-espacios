package co.edu.uis.reservas.config;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class HorasReservaTest {

    @Test
    void soloEnPuntoOYMedia() {
        assertTrue(HorasReserva.esValida(LocalDateTime.of(2026, 11, 20, 13, 0)));
        assertTrue(HorasReserva.esValida(LocalDateTime.of(2026, 11, 20, 13, 30)));
        assertFalse(HorasReserva.esValida(LocalDateTime.of(2026, 11, 20, 13, 15)));
        assertFalse(HorasReserva.esValida(LocalDateTime.of(2026, 11, 20, 13, 59)));
        assertFalse(HorasReserva.esValida(LocalDateTime.of(2026, 11, 20, 13, 30, 10)));
    }
}
