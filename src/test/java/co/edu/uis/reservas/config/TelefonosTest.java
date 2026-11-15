package co.edu.uis.reservas.config;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TelefonosTest {

    private static Optional<String> f(String t) { return Telefonos.aE164(t, "+57"); }

    @Test
    void numeroColombianoLlevaCodigoDePais() {
        assertEquals(Optional.of("+573001234567"), f("3001234567"));
        assertEquals(Optional.of("+573001234567"), f("300 123 4567"));
        assertEquals(Optional.of("+573001234567"), f("300-123-4567"));
    }

    @Test
    void respetaCodigoDePaisYaEscrito() {
        assertEquals(Optional.of("+573001234567"), f("+57 300 123 4567"));
        assertEquals(Optional.of("+573001234567"), f("573001234567"));
        assertEquals(Optional.of("+573001234567"), f("0057 300 1234567"));
        assertEquals(Optional.of("+14155552671"), f("+1 415 555 2671"));
    }

    @Test
    void numerosInvalidosDanVacio() {
        assertEquals(Optional.empty(), f(null));
        assertEquals(Optional.empty(), f("   "));
        assertEquals(Optional.empty(), f("abc"));
        assertEquals(Optional.empty(), f("123"));
        assertEquals(Optional.empty(), f("+0123456789"));
    }
}
