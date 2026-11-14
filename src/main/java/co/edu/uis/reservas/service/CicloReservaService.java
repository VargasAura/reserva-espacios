package co.edu.uis.reservas.service;

import co.edu.uis.reservas.config.ReglasNegocio;
import co.edu.uis.reservas.domain.EstadoReserva;
import co.edu.uis.reservas.domain.HistorialEstadoReserva;
import co.edu.uis.reservas.domain.Reserva;
import co.edu.uis.reservas.domain.TipoNotificacion;
import co.edu.uis.reservas.exception.ReglaNegocioException;
import co.edu.uis.reservas.repository.CicloReservaQueryRepository;
import co.edu.uis.reservas.repository.HistorialEstadoReservaRepository;
import co.edu.uis.reservas.repository.NotificacionRepository;
import co.edu.uis.reservas.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Ciclo de vida de la reserva después de confirmada: EN_USO, FINALIZADA, NO_SHOW y liberación por plazo vencido.
 * REQ-26: el horario se libera solo porque la reserva deja de estar en EstadoReserva.ACTIVAS.
 */
@Service
public class CicloReservaService {

    /** Responsable que se registra cuando el cambio lo hace una tarea automática (REQ-27). */
    public static final String RESPONSABLE_SISTEMA = "SISTEMA";

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    /** Una fila de la pantalla del administrador. */
    public record FilaUso(Long id, String usuario, String espacio, String fecha, String horario,
                          String estado, String estadoTexto, boolean puedeIniciar, boolean puedeFinalizar) {}

    private final ReservaRepository reservaRepository;
    private final CicloReservaQueryRepository consultas;
    private final HistorialEstadoReservaRepository historialRepository;
    private final NotificacionRepository notificacionRepository;
    private final NotificacionService notificacionService;

    public CicloReservaService(ReservaRepository reservaRepository, CicloReservaQueryRepository consultas,
                               HistorialEstadoReservaRepository historialRepository,
                               NotificacionRepository notificacionRepository,
                               NotificacionService notificacionService) {
        this.reservaRepository = reservaRepository;
        this.consultas = consultas;
        this.historialRepository = historialRepository;
        this.notificacionRepository = notificacionRepository;
        this.notificacionService = notificacionService;
    }

    // REQ-23, REQ-24: reservas CONFIRMADAS (se pueden iniciar) y EN_USO (se pueden finalizar) para el administrador
    @Transactional(readOnly = true)
    public List<FilaUso> filasParaAdmin() {
        return consultas.findByEstadoInOrderByInicioAsc(List.of(EstadoReserva.CONFIRMADA, EstadoReserva.EN_USO))
                .stream().map(this::aFila).toList();
    }

    // REQ-23, REQ-27: el administrador registra el inicio del uso (CONFIRMADA -> EN_USO)
    @Transactional
    public void iniciarUso(Long id, String responsable) {
        Reserva r = obtener(id);
        if (r.getEstado() != EstadoReserva.CONFIRMADA) {
            throw new ReglaNegocioException("Solo se puede iniciar el uso de una reserva CONFIRMADA.");
        }
        cambiarEstado(r, EstadoReserva.EN_USO, responsable);
    }

    // REQ-24, REQ-26, REQ-27: el administrador registra la finalización (EN_USO -> FINALIZADA)
    @Transactional
    public void finalizar(Long id, String responsable) {
        Reserva r = obtener(id);
        if (r.getEstado() != EstadoReserva.EN_USO) {
            throw new ReglaNegocioException("Solo se puede finalizar una reserva EN_USO.");
        }
        cambiarEstado(r, EstadoReserva.FINALIZADA, responsable);
    }

    // REQ-18: reservas RESERVADA que ya pasaron el plazo de confirmación
    @Transactional(readOnly = true)
    public List<Long> idsReservadasVencidas() {
        LocalDateTime limite = LocalDateTime.now().minus(ReglasNegocio.PLAZO_CONFIRMACION);
        return ids(consultas.findByEstadoAndCreadaEnBefore(EstadoReserva.RESERVADA, limite));
    }

    // REQ-18, REQ-26, REQ-27, REQ-35: libera la reserva vencida (queda CANCELADA) e informa al usuario
    @Transactional
    public void liberarPorVencimiento(Long id) {
        Reserva r = obtener(id);
        boolean vencida = r.getCreadaEn().plus(ReglasNegocio.PLAZO_CONFIRMACION).isBefore(LocalDateTime.now());
        if (r.getEstado() != EstadoReserva.RESERVADA || !vencida) return; // ya cambió: no hacer nada
        cambiarEstado(r, EstadoReserva.CANCELADA, RESPONSABLE_SISTEMA);
        notificacionService.notificar(r, TipoNotificacion.CANCELACION,
                "Tu reserva en " + descripcion(r) + " fue liberada porque no la confirmaste dentro de "
                        + texto(ReglasNegocio.PLAZO_CONFIRMACION) + ".");
    }

    // REQ-25: reservas CONFIRMADAS cuyo inicio ya pasó más que la tolerancia
    @Transactional(readOnly = true)
    public List<Long> idsConfirmadasSinUso() {
        LocalDateTime limite = LocalDateTime.now().minus(ReglasNegocio.TOLERANCIA_NO_SHOW);
        return ids(consultas.findByEstadoAndInicioBefore(EstadoReserva.CONFIRMADA, limite));
    }

    // REQ-25, REQ-26, REQ-27: marca NO_SHOW si venció la tolerancia y el uso no se inició
    @Transactional
    public void marcarNoShow(Long id) {
        Reserva r = obtener(id);
        boolean vencida = r.getInicio().plus(ReglasNegocio.TOLERANCIA_NO_SHOW).isBefore(LocalDateTime.now());
        if (r.getEstado() != EstadoReserva.CONFIRMADA || !vencida) return;
        cambiarEstado(r, EstadoReserva.NO_SHOW, RESPONSABLE_SISTEMA);
    }

    // REQ-24: reservas EN_USO cuya hora de fin ya llegó
    @Transactional(readOnly = true)
    public List<Long> idsEnUsoTerminadas() {
        return ids(consultas.findByEstadoAndFinLessThanEqual(EstadoReserva.EN_USO, LocalDateTime.now()));
    }

    // REQ-24, REQ-26, REQ-27: finaliza automáticamente al llegar la hora de fin
    @Transactional
    public void finalizarAutomaticamente(Long id) {
        Reserva r = obtener(id);
        if (r.getEstado() != EstadoReserva.EN_USO || r.getFin().isAfter(LocalDateTime.now())) return;
        cambiarEstado(r, EstadoReserva.FINALIZADA, RESPONSABLE_SISTEMA);
    }

    // REQ-34: reservas CONFIRMADAS que empiezan dentro de la anticipación del recordatorio
    @Transactional(readOnly = true)
    public List<Long> idsParaRecordatorio() {
        LocalDateTime ahora = LocalDateTime.now();
        return ids(consultas.findByEstadoAndInicioBetween(EstadoReserva.CONFIRMADA, ahora,
                ahora.plus(ReglasNegocio.ANTICIPACION_RECORDATORIO)));
    }

    // REQ-34, REQ-37: envía el recordatorio una sola vez por reserva
    @Transactional
    public void enviarRecordatorio(Long id) {
        Reserva r = obtener(id);
        if (r.getEstado() != EstadoReserva.CONFIRMADA) return;
        boolean yaAvisada = notificacionRepository.findByReservaIdOrderByFechaEnvioDesc(id).stream()
                .anyMatch(n -> n.getTipo() == TipoNotificacion.RECORDATORIO);
        if (yaAvisada) return;
        notificacionService.notificar(r, TipoNotificacion.RECORDATORIO,
                "Recordatorio: tienes una reserva en " + descripcion(r) + ".");
    }

    // REQ-27: guarda un cambio de estado. Público para que ReservaService (Chat 3) lo use al crear, confirmar y cancelar
    @Transactional
    public void registrarCambio(Reserva reserva, EstadoReserva anterior, EstadoReserva nuevo, String responsable) {
        historialRepository.save(new HistorialEstadoReserva(reserva, anterior, nuevo, responsable));
    }

    // REQ-26, REQ-27: cambia el estado y deja el registro (al salir de ACTIVAS el horario queda libre)
    private void cambiarEstado(Reserva r, EstadoReserva nuevo, String responsable) {
        EstadoReserva anterior = r.getEstado();
        r.setEstado(nuevo);
        reservaRepository.save(r);
        registrarCambio(r, anterior, nuevo, responsable);
    }

    // REQ-23, REQ-24: busca la reserva o avisa que no existe
    private Reserva obtener(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new ReglaNegocioException("La reserva no existe."));
    }

    // REQ-18, REQ-24, REQ-25, REQ-34: solo los números de reserva (cada una se procesa en su propia transacción)
    private List<Long> ids(List<Reserva> reservas) {
        return reservas.stream().map(Reserva::getId).toList();
    }

    // REQ-23, REQ-24: convierte una reserva en una fila de texto para la pantalla del administrador
    private FilaUso aFila(Reserva r) {
        String estado = r.getEstado().name();
        return new FilaUso(r.getId(), r.getUsuario().getNombre(), r.getEspacio().getNombre(),
                FECHA.format(r.getInicio()), HORA.format(r.getInicio()) + " – " + HORA.format(r.getFin()),
                estado, estado.replace('_', ' '),
                r.getEstado() == EstadoReserva.CONFIRMADA, r.getEstado() == EstadoReserva.EN_USO);
    }

    // REQ-18, REQ-34, REQ-37: texto de la reserva para los mensajes
    private String descripcion(Reserva r) {
        return "«" + r.getEspacio().getNombre() + "» el " + FECHA.format(r.getInicio())
                + " de " + HORA.format(r.getInicio()) + " a " + HORA.format(r.getFin());
    }

    // REQ-18: convierte una duración de las reglas de negocio en texto ("30 minutos", "2 horas")
    private static String texto(Duration d) {
        long minutos = d.toMinutes();
        if (minutos % 60 == 0) {
            long horas = minutos / 60;
            return horas + (horas == 1 ? " hora" : " horas");
        }
        return minutos + " minutos";
    }
}