package co.edu.uis.reservas.service;

import co.edu.uis.reservas.domain.Espacio;
import co.edu.uis.reservas.domain.EstadoReserva;
import co.edu.uis.reservas.domain.Notificacion;
import co.edu.uis.reservas.domain.Reserva;
import co.edu.uis.reservas.domain.TipoNotificacion;
import co.edu.uis.reservas.repository.CicloReservaQueryRepository;
import co.edu.uis.reservas.repository.HistorialEstadoReservaRepository;
import co.edu.uis.reservas.repository.NotificacionRepository;
import co.edu.uis.reservas.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Recordatorio por SMS cuando pasan 30 minutos del inicio y el espacio no se marcó como ocupado. */
class CicloReservaServiceRecordatorioTest {

    private ReservaRepository reservaRepository;
    private NotificacionRepository notificacionRepository;
    private NotificacionService notificacionService;
    private CicloReservaService ciclo;

    @BeforeEach
    void preparar() {
        reservaRepository = mock(ReservaRepository.class);
        notificacionRepository = mock(NotificacionRepository.class);
        notificacionService = mock(NotificacionService.class);
        ciclo = new CicloReservaService(reservaRepository, mock(CicloReservaQueryRepository.class),
                mock(HistorialEstadoReservaRepository.class), notificacionRepository, notificacionService);
        when(notificacionRepository.findByReservaIdOrderByFechaEnvioDesc(5L)).thenReturn(List.of());
    }

    private Reserva reserva(EstadoReserva estado, int minutosDesdeElInicio) {
        Espacio espacio = mock(Espacio.class);
        when(espacio.getNombre()).thenReturn("Sala 1");
        LocalDateTime inicio = LocalDateTime.now().minusMinutes(minutosDesdeElInicio);
        Reserva r = mock(Reserva.class);
        when(r.getId()).thenReturn(5L);
        when(r.getEstado()).thenReturn(estado);
        when(r.getEspacio()).thenReturn(espacio);
        when(r.getInicio()).thenReturn(inicio);
        when(r.getFin()).thenReturn(inicio.plusHours(1));
        when(reservaRepository.findById(5L)).thenReturn(Optional.of(r));
        return r;
    }

    @Test
    void aLos31MinutosSinOcuparSeRecuerda() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA, 31);
        ciclo.enviarRecordatorioSinOcupar(5L);
        verify(notificacionService).notificar(eq(r), eq(TipoNotificacion.RECORDATORIO),
                contains("aún no está marcado como ocupado"));
    }

    @Test
    void antesDeLos30MinutosNoSeRecuerda() {
        reserva(EstadoReserva.CONFIRMADA, 10);
        ciclo.enviarRecordatorioSinOcupar(5L);
        verify(notificacionService, never()).notificar(any(), any(), any());
    }

    @Test
    void siYaLeTocaNoShowNoSeRecuerda() {
        reserva(EstadoReserva.CONFIRMADA, 50);
        ciclo.enviarRecordatorioSinOcupar(5L);
        verify(notificacionService, never()).notificar(any(), any(), any());
    }

    @Test
    void siYaSeMarcoOcupadoNoSeRecuerda() {
        reserva(EstadoReserva.EN_USO, 31);
        ciclo.enviarRecordatorioSinOcupar(5L);
        verify(notificacionService, never()).notificar(any(), any(), any());
    }

    @Test
    void noSeRepiteElRecordatorio() {
        reserva(EstadoReserva.CONFIRMADA, 31);
        Notificacion previa = new Notificacion();
        previa.setTipo(TipoNotificacion.RECORDATORIO);
        previa.setFechaEnvio(LocalDateTime.now().minusMinutes(1)); // enviado después del inicio
        when(notificacionRepository.findByReservaIdOrderByFechaEnvioDesc(5L)).thenReturn(List.of(previa));
        ciclo.enviarRecordatorioSinOcupar(5L);
        verify(notificacionService, never()).notificar(any(), any(), any());
    }

    @Test
    void elRecordatorioDe24HorasNoBloqueaElDeLos30Minutos() {
        reserva(EstadoReserva.CONFIRMADA, 31);
        Notificacion previa = new Notificacion();
        previa.setTipo(TipoNotificacion.RECORDATORIO);
        previa.setFechaEnvio(LocalDateTime.now().minusHours(20)); // enviado antes del inicio
        when(notificacionRepository.findByReservaIdOrderByFechaEnvioDesc(5L)).thenReturn(List.of(previa));
        ciclo.enviarRecordatorioSinOcupar(5L);
        verify(notificacionService).notificar(any(), eq(TipoNotificacion.RECORDATORIO), any());
    }
}
