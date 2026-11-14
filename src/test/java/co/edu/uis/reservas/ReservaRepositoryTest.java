package co.edu.uis.reservas;

import co.edu.uis.reservas.domain.*;
import co.edu.uis.reservas.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Prueba de REQ-28: detección de superposición de horarios. */
@DataJpaTest
class ReservaRepositoryTest {

    @Autowired UsuarioRepository usuarios;
    @Autowired EspacioRepository espacios;
    @Autowired ReservaRepository reservas;

    Espacio sala;
    final LocalDateTime base = LocalDateTime.of(2030, 1, 10, 10, 0);

    @BeforeEach
    void preparar() {
        Usuario u = new Usuario();
        u.setNombre("Test"); u.setEmail("t@t.co"); u.setPassword("x");
        usuarios.save(u);
        sala = espacios.save(new Espacio("Sala Test", TipoEspacio.SALA_REUNIONES, 10, "A1"));

        Reserva r = new Reserva();
        r.setUsuario(u); r.setEspacio(sala);
        r.setInicio(base); r.setFin(base.plusHours(2));   // 10:00 - 12:00
        r.setEstado(EstadoReserva.CONFIRMADA);
        reservas.save(r);
    }

    @Test
    void horarioSuperpuestoEsConflicto() {
        assertTrue(reservas.existeSolape(sala.getId(), base.plusHours(1), base.plusHours(3), EstadoReserva.ACTIVAS, -1L));
    }

    @Test
    void reservaConsecutivaNoEsConflicto() {
        assertFalse(reservas.existeSolape(sala.getId(), base.plusHours(2), base.plusHours(3), EstadoReserva.ACTIVAS, -1L));
    }
}
