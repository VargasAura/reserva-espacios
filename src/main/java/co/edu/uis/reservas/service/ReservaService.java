package co.edu.uis.reservas.service;

import co.edu.uis.reservas.config.ReglasNegocio;
import co.edu.uis.reservas.domain.*;
import co.edu.uis.reservas.dto.ReservaForm;
import co.edu.uis.reservas.exception.ReglaNegocioException;
import co.edu.uis.reservas.repository.EspacioRepository;
import co.edu.uis.reservas.repository.ReservaQueryRepository;
import co.edu.uis.reservas.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ReservaService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    /** existeSolape pide un id para ignorar; -1 = no ignorar ninguna (reserva nueva). */
    private static final Long SIN_EXCLUIR = -1L;

    private final ReservaRepository reservaRepository;
    private final ReservaQueryRepository reservaQueryRepository;
    private final EspacioRepository espacioRepository;
    private final UsuarioService usuarioService;
    private final NotificacionService notificacionService;

    public ReservaService(ReservaRepository reservaRepository, ReservaQueryRepository reservaQueryRepository,
                          EspacioRepository espacioRepository, UsuarioService usuarioService,
                          NotificacionService notificacionService) {
        this.reservaRepository = reservaRepository;
        this.reservaQueryRepository = reservaQueryRepository;
        this.espacioRepository = espacioRepository;
        this.usuarioService = usuarioService;
        this.notificacionService = notificacionService;
    }

    // REQ-15: espacios que se pueden elegir en el formulario (solo activos)
    @Transactional(readOnly = true)
    public List<Espacio> espaciosDisponibles() {
        return espacioRepository.findByActivoTrueOrderByNombreAsc();
    }

    // REQ-15, REQ-16, REQ-28, REQ-29, REQ-30, REQ-31: crea la reserva en estado RESERVADA reteniendo el horario
    @Transactional
    public Reserva crear(String email, ReservaForm form) {
        Usuario usuario = usuarioService.buscarPorEmail(email);
        LocalDateTime inicio = form.calcularInicio();
        LocalDateTime fin = form.calcularFin();
        validarHorario(inicio, fin);

        Espacio espacio = bloquearEspacio(form.getEspacioId());
        verificarDisponibilidad(espacio, inicio, fin, SIN_EXCLUIR, "");

        Reserva reserva = new Reserva();
        reserva.setUsuario(usuario);
        reserva.setEspacio(espacio);
        reserva.setInicio(inicio);
        reserva.setFin(fin);
        reserva.setEstado(EstadoReserva.RESERVADA);
        return reservaRepository.save(reserva);
    }

    // REQ-17, REQ-28, REQ-30, REQ-31: RESERVADA -> CONFIRMADA solo si el espacio sigue disponible
    @Transactional
    public Reserva confirmar(Long id, String email) {
        Reserva reserva = obtenerPropia(id, email);
        if (reserva.getEstado() != EstadoReserva.RESERVADA) {
            throw new ReglaNegocioException("Solo se puede confirmar una reserva en estado RESERVADA.");
        }
        Espacio espacio = bloquearEspacio(reserva.getEspacio().getId());
        verificarDisponibilidad(espacio, reserva.getInicio(), reserva.getFin(), reserva.getId(),
                " La reserva sigue en estado RESERVADA.");

        reserva.setEstado(EstadoReserva.CONFIRMADA);
        Reserva guardada = reservaRepository.save(reserva);
        notificacionService.notificar(guardada, TipoNotificacion.CONFIRMACION,
                "Tu reserva en " + descripcion(guardada) + " fue confirmada.");
        return guardada;
    }

    // REQ-21, REQ-16, REQ-28, REQ-30, REQ-31: cambia fecha y horario; si hay conflicto la reserva original no se toca
    @Transactional
    public Reserva editar(Long id, String email, ReservaForm form) {
        Reserva reserva = obtenerPropia(id, email);
        exigirEditable(reserva);
        LocalDateTime inicio = form.calcularInicio();
        LocalDateTime fin = form.calcularFin();
        validarHorario(inicio, fin);

        Espacio espacio = bloquearEspacio(reserva.getEspacio().getId());
        verificarDisponibilidad(espacio, inicio, fin, reserva.getId(),
                " Tu reserva original se conserva sin cambios.");

        reserva.setInicio(inicio);
        reserva.setFin(fin);
        Reserva guardada = reservaRepository.save(reserva);
        notificacionService.notificar(guardada, TipoNotificacion.MODIFICACION,
                "Tu reserva cambió de horario: " + descripcion(guardada) + ".");
        return guardada;
    }

    // REQ-19, REQ-20, REQ-32: cancela RESERVADA (siempre) o CONFIRMADA (con la anticipación mínima); libera el horario
    @Transactional
    public Reserva cancelar(Long id, String email) {
        Reserva reserva = obtenerPropia(id, email);
        if (reserva.getEstado() == EstadoReserva.CONFIRMADA) {
            LocalDateTime limite = reserva.getInicio().minus(ReglasNegocio.ANTICIPACION_CANCELACION);
            if (LocalDateTime.now().isAfter(limite)) {
                throw new ReglaNegocioException("No se puede cancelar: una reserva confirmada solo se cancela con al menos "
                        + texto(ReglasNegocio.ANTICIPACION_CANCELACION) + " de anticipación.");
            }
        } else if (reserva.getEstado() != EstadoReserva.RESERVADA) {
            throw new ReglaNegocioException("Esta reserva no se puede cancelar (estado: " + reserva.getEstado() + ").");
        }
        reserva.setEstado(EstadoReserva.CANCELADA); // al dejar de ser activa, existeSolape ya no la cuenta (REQ-32)
        Reserva guardada = reservaRepository.save(reserva);
        notificacionService.notificar(guardada, TipoNotificacion.CANCELACION,
                "Tu reserva en " + descripcion(guardada) + " fue cancelada.");
        return guardada;
    }

    // REQ-22: mis reservas, con filtro opcional por estado (estado = null muestra todas)
    @Transactional(readOnly = true)
    public List<Reserva> misReservas(String email, EstadoReserva estado) {
        Long usuarioId = usuarioService.buscarPorEmail(email).getId();
        return (estado == null)
                ? reservaQueryRepository.misReservas(usuarioId)
                : reservaQueryRepository.misReservasPorEstado(usuarioId, estado);
    }

    // REQ-21: datos de una reserva propia para llenar el formulario de edición
    @Transactional(readOnly = true)
    public ReservaForm formularioParaEditar(Long id, String email) {
        Reserva reserva = obtenerPropia(id, email);
        exigirEditable(reserva);
        return ReservaForm.desde(reserva);
    }

    // REQ-21, REQ-22: solo el dueño puede ver o modificar su reserva
    private Reserva obtenerPropia(Long id, String email) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new ReglaNegocioException("La reserva no existe."));
        if (!reserva.getUsuario().getEmail().equalsIgnoreCase(email)) {
            throw new ReglaNegocioException("La reserva no existe.");
        }
        return reserva;
    }

    // REQ-21: solo se editan reservas RESERVADA o CONFIRMADA
    private void exigirEditable(Reserva reserva) {
        if (reserva.getEstado() != EstadoReserva.RESERVADA && reserva.getEstado() != EstadoReserva.CONFIRMADA) {
            throw new ReglaNegocioException("Solo se puede editar una reserva RESERVADA o CONFIRMADA.");
        }
    }

    // REQ-16: inicio no anterior a ahora, fin posterior al inicio y duración dentro de los límites
    private void validarHorario(LocalDateTime inicio, LocalDateTime fin) {
        if (inicio.isBefore(LocalDateTime.now())) {
            throw new ReglaNegocioException("La fecha y la hora de inicio no pueden ser anteriores al momento actual.");
        }
        if (!fin.isAfter(inicio)) {
            throw new ReglaNegocioException("La hora de fin debe ser posterior a la hora de inicio.");
        }
        Duration duracion = Duration.between(inicio, fin);
        if (duracion.compareTo(ReglasNegocio.DURACION_MINIMA) < 0) {
            throw new ReglaNegocioException("La duración mínima de una reserva es de "
                    + texto(ReglasNegocio.DURACION_MINIMA) + ".");
        }
        if (duracion.compareTo(ReglasNegocio.DURACION_MAXIMA) > 0) {
            throw new ReglaNegocioException("La duración máxima de una reserva es de "
                    + texto(ReglasNegocio.DURACION_MAXIMA) + ".");
        }
    }

    // REQ-31: bloquea la fila del espacio; otra reserva simultánea del mismo espacio espera su turno
    private Espacio bloquearEspacio(Long espacioId) {
        return espacioRepository.findByIdForUpdate(espacioId)
                .filter(Espacio::isActivo)
                .orElseThrow(() -> new ReglaNegocioException("El espacio no existe o no está disponible."));
    }

    // REQ-28, REQ-30: si el horario se cruza con otra reserva activa, rechaza y muestra los horarios libres del día
    private void verificarDisponibilidad(Espacio espacio, LocalDateTime inicio, LocalDateTime fin,
                                         Long excluirId, String consecuencia) {
        if (!reservaRepository.existeSolape(espacio.getId(), inicio, fin, EstadoReserva.ACTIVAS, excluirId)) {
            return;
        }
        LocalDate dia = inicio.toLocalDate();
        List<String> libres = horariosLibres(espacio.getId(), dia, excluirId);
        String libresTexto = libres.isEmpty()
                ? "No quedan horarios libres ese día."
                : "Horarios libres ese día: " + String.join(", ", libres) + ".";
        throw new ReglaNegocioException("El horario no está disponible: «" + espacio.getNombre()
                + "» ya tiene una reserva que se cruza con " + HORA.format(inicio) + "–" + HORA.format(fin)
                + " del " + FECHA.format(dia) + ". " + libresTexto + consecuencia);
    }

    // REQ-30: tramos libres del espacio en un día (si es hoy, desde ahora; solo tramos que alcanzan la duración mínima)
    private List<String> horariosLibres(Long espacioId, LocalDate dia, Long excluirId) {
        LocalDateTime inicioDia = dia.atStartOfDay();
        LocalDateTime finDia = dia.plusDays(1).atStartOfDay();
        LocalDateTime ahora = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime cursor = ahora.isAfter(inicioDia) ? ahora : inicioDia;

        List<Reserva> ocupadas = new ArrayList<>(reservaRepository
                .findByEspacioIdAndEstadoInAndInicioLessThanAndFinGreaterThan(
                        espacioId, EstadoReserva.ACTIVAS, finDia, inicioDia));
        ocupadas.removeIf(r -> r.getId().equals(excluirId));
        ocupadas.sort(Comparator.comparing(Reserva::getInicio));

        List<String> libres = new ArrayList<>();
        for (Reserva r : ocupadas) {
            if (r.getInicio().isAfter(cursor)) agregarSiCabe(libres, cursor, r.getInicio(), dia);
            if (r.getFin().isAfter(cursor)) cursor = r.getFin();
        }
        if (finDia.isAfter(cursor)) agregarSiCabe(libres, cursor, finDia, dia);
        return libres;
    }

    // REQ-30: agrega el tramo a la lista solo si dura al menos la duración mínima de una reserva
    private void agregarSiCabe(List<String> libres, LocalDateTime desde, LocalDateTime hasta, LocalDate dia) {
        if (Duration.between(desde, hasta).compareTo(ReglasNegocio.DURACION_MINIMA) >= 0) {
            String fin = hasta.toLocalDate().isAfter(dia) ? "24:00" : HORA.format(hasta);
            libres.add(HORA.format(desde) + "–" + fin);
        }
    }

    // REQ-17, REQ-21, REQ-20: texto de la reserva para los avisos y notificaciones
    private String descripcion(Reserva r) {
        return "«" + r.getEspacio().getNombre() + "» el " + FECHA.format(r.getInicio())
                + " de " + HORA.format(r.getInicio()) + " a " + HORA.format(r.getFin());
    }

    // REQ-16, REQ-20: convierte una duración de las reglas de negocio en texto ("2 horas", "30 minutos")
    private static String texto(Duration d) {
        long minutos = d.toMinutes();
        if (minutos % 60 == 0) {
            long horas = minutos / 60;
            return horas + (horas == 1 ? " hora" : " horas");
        }
        return minutos + " minutos";
    }
}