package co.edu.uis.reservas.service;

import co.edu.uis.reservas.domain.Espacio;
import co.edu.uis.reservas.domain.Reserva;
import co.edu.uis.reservas.domain.Usuario;
import co.edu.uis.reservas.dto.ReservaForm;
import co.edu.uis.reservas.exception.ReglaNegocioException;
import co.edu.uis.reservas.repository.EspacioRepository;
import co.edu.uis.reservas.repository.ReservaQueryRepository;
import co.edu.uis.reservas.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Las horas de una reserva solo pueden ser en punto o y media. */
class ReservaServiceHorasTest {

    private static final String EMAIL = "ana@test.co";

    private ReservaRepository reservaRepository;
    private ReservaService servicio;
    private LocalDate manana;

    @BeforeEach
    void preparar() {
        reservaRepository = mock(ReservaRepository.class);
        EspacioRepository espacioRepository = mock(EspacioRepository.class);
        UsuarioService usuarioService = mock(UsuarioService.class);

        Espacio espacio = mock(Espacio.class);
        when(espacio.isActivo()).thenReturn(true);
        when(espacioRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(espacio));
        when(usuarioService.buscarPorEmail(EMAIL)).thenReturn(new Usuario());
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(i -> i.getArgument(0));

        servicio = new ReservaService(reservaRepository, mock(ReservaQueryRepository.class), espacioRepository,
                usuarioService, mock(NotificacionService.class));
        manana = LocalDate.now().plusDays(1);
    }

    private ReservaForm form(LocalTime inicio, LocalTime fin) {
        ReservaForm f = new ReservaForm();
        f.setEspacioId(1L);
        f.setFecha(manana);
        f.setHoraInicio(inicio);
        f.setHoraFin(fin);
        return f;
    }

    @Test
    void inicioConMinutosRarosSeRechaza() {
        ReglaNegocioException e = assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(EMAIL, form(LocalTime.of(10, 15), LocalTime.of(11, 0))));
        assertTrue(e.getMessage().contains("en punto o y media"));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void finConMinutosRarosSeRechaza() {
        assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(EMAIL, form(LocalTime.of(10, 0), LocalTime.of(10, 45))));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void enPuntoYMediaSeAcepta() {
        Reserva r = servicio.crear(EMAIL, form(LocalTime.of(13, 30), LocalTime.of(15, 0)));
        assertEquals(manana.atTime(13, 30), r.getInicio());
        assertEquals(manana.atTime(15, 0), r.getFin());
    }

    @Test
    void puedeTerminarALaMedianoche() {
        Reserva r = servicio.crear(EMAIL, form(LocalTime.of(22, 0), LocalTime.MIDNIGHT));
        assertEquals(manana.plusDays(1).atStartOfDay(), r.getFin());
    }
}
