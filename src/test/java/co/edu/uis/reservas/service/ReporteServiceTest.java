
package co.edu.uis.reservas.service;

import co.edu.uis.reservas.domain.Espacio;
import co.edu.uis.reservas.domain.EstadoReserva;
import co.edu.uis.reservas.domain.Reserva;
import co.edu.uis.reservas.domain.TipoEspacio;
import co.edu.uis.reservas.exception.ReglaNegocioException;
import co.edu.uis.reservas.repository.ReporteQueryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Pruebas de los reportes (REQ-52, REQ-53, REQ-44, REQ-55). Periodo de ejemplo: 1 al 10 de enero de 2026 = 10 días = 160 h. */
@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 1, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 1, 10);

    @Mock
    private ReporteQueryRepository repo;

    private Reserva reserva(Espacio espacio, int dia, int horaInicio, int horaFin, EstadoReserva estado) {
        Reserva r = new Reserva();
        r.setEspacio(espacio);
        r.setInicio(LocalDateTime.of(2026, 1, dia, horaInicio, 0));
        r.setFin(LocalDateTime.of(2026, 1, dia, horaFin, 0));
        r.setEstado(estado);
        return r;
    }

    private ReporteService servicio(List<Espacio> espacios, List<Reserva> reservas) {
        when(repo.todosLosEspacios()).thenReturn(espacios);
        when(repo.reservasEnPeriodo(any(), any())).thenReturn(reservas);
        return new ReporteService(repo);
    }

    @Test
    void calculaHorasYPorcentajeDeOcupacion() {
        Espacio sala = new Espacio("Sala A", TipoEspacio.SALA_REUNIONES, 10, "Bloque 1");
        Espacio aula = new Espacio("Sala B", TipoEspacio.SALA_REUNIONES, 8, "Bloque 2");
        var reporte = servicio(List.of(sala, aula), List.of(
                reserva(sala, 2, 8, 16, EstadoReserva.FINALIZADA),
                reserva(sala, 3, 8, 16, EstadoReserva.CONFIRMADA)))
                .generar(DESDE, HASTA, null, null);

        var fila = reporte.filas().get(0); // la más ocupada va primero
        assertEquals("Sala A", fila.nombre());
        assertEquals(2, fila.totalReservas());
        assertEquals(16.0, fila.horasReservadas());
        assertEquals(10.0, fila.ocupacion());       // 16 h / 160 h
        assertEquals(5.0, reporte.ocupacionGlobal()); // 16 h / (2 espacios × 160 h)
    }

    @Test
    void cuentaReservasPorEstado() {
        Espacio sala = new Espacio("Sala A", TipoEspacio.SALA_REUNIONES, 10, "Bloque 1");
        var reporte = servicio(List.of(sala), List.of(
                reserva(sala, 2, 8, 9, EstadoReserva.FINALIZADA),
                reserva(sala, 3, 8, 9, EstadoReserva.FINALIZADA),
                reserva(sala, 4, 8, 9, EstadoReserva.FINALIZADA),
                reserva(sala, 5, 8, 9, EstadoReserva.CANCELADA),
                reserva(sala, 6, 8, 9, EstadoReserva.CANCELADA),
                reserva(sala, 7, 8, 9, EstadoReserva.NO_SHOW)))
                .generar(DESDE, HASTA, null, null);

        assertEquals(3, reporte.estados().get(0).cantidad()); // Finalizadas
        assertEquals(2, reporte.estados().get(1).cantidad()); // Canceladas
        assertEquals(1, reporte.estados().get(2).cantidad()); // No asistió
        assertEquals(50.0, reporte.estados().get(0).porcentaje());
    }

    @Test
    void canceladasYNoShowNoSumanHoras() {
        Espacio sala = new Espacio("Sala A", TipoEspacio.SALA_REUNIONES, 10, "Bloque 1");
        var reporte = servicio(List.of(sala), List.of(
                reserva(sala, 2, 8, 12, EstadoReserva.CANCELADA),
                reserva(sala, 3, 8, 12, EstadoReserva.NO_SHOW)))
                .generar(DESDE, HASTA, null, null);

        assertEquals(2, reporte.totalReservas());
        assertEquals(0.0, reporte.horasReservadas());
    }

    @Test
    void sinReservasNoHayDatos() {
        Espacio sala = new Espacio("Sala A", TipoEspacio.SALA_REUNIONES, 10, "Bloque 1");
        var reporte = servicio(List.of(sala), List.of()).generar(DESDE, HASTA, null, null);

        assertFalse(reporte.hayDatos()); // la pantalla mostrará el mensaje informativo
    }

    @Test
    void incluyeEspacioEliminadoSoloSiTieneReservas() {
        Espacio eliminadoConHistorial = new Espacio("Auditorio viejo", TipoEspacio.AUDITORIO, 100, "Bloque 3");
        eliminadoConHistorial.setActivo(false);
        Espacio eliminadoSinReservas = new Espacio("Cancha vieja", TipoEspacio.CANCHA_DEPORTIVA, 20, "Patio");
        eliminadoSinReservas.setActivo(false);

        var reporte = servicio(List.of(eliminadoConHistorial, eliminadoSinReservas), List.of(
                reserva(eliminadoConHistorial, 2, 8, 10, EstadoReserva.FINALIZADA)))
                .generar(DESDE, HASTA, null, null);

        assertEquals(1, reporte.filas().size());
        assertTrue(reporte.filas().get(0).eliminado());
    }

    @Test
    void rechazaRangoDeFechasInvertido() {
        var servicio = new ReporteService(repo);
        assertThrows(ReglaNegocioException.class, () -> servicio.generar(HASTA, DESDE, null, null));
    }
}