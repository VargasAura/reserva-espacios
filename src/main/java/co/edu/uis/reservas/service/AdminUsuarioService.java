package co.edu.uis.reservas.service;

import co.edu.uis.reservas.domain.EstadoReserva;
import co.edu.uis.reservas.domain.Reserva;
import co.edu.uis.reservas.domain.Rol;
import co.edu.uis.reservas.domain.TipoNotificacion;
import co.edu.uis.reservas.domain.Usuario;
import co.edu.uis.reservas.exception.ReglaNegocioException;
import co.edu.uis.reservas.repository.ReservaRepository;
import co.edu.uis.reservas.repository.UsuarioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AdminUsuarioService {

    /** REQ-50: siempre debe quedar al menos esta cantidad de administradores activos. */
    private static final int MINIMO_ADMINS_ACTIVOS = 1;

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    /** Una fila de la tabla de usuarios; rol = "ADMIN" o "USUARIO". */
    public record FilaUsuario(Long id, String nombre, String email, String telefono, String rol, boolean activo) {}

    /** Una reserva de un usuario, con la marca de si el administrador puede cancelarla. */
    public record FilaReserva(Long id, String espacio, String fecha, String horario,
                              String estado, String estadoTexto, boolean cancelable) {}

    public record ReservasDeUsuario(Long usuarioId, String nombre, List<FilaReserva> reservas) {}

    private final UsuarioRepository usuarioRepository;
    private final ReservaRepository reservaRepository;
    private final NotificacionService notificacionService;

    public AdminUsuarioService(UsuarioRepository usuarioRepository, ReservaRepository reservaRepository,
                               NotificacionService notificacionService) {
        this.usuarioRepository = usuarioRepository;
        this.reservaRepository = reservaRepository;
        this.notificacionService = notificacionService;
    }

    // REQ-46: lista y busca usuarios por nombre o correo (y opcionalmente por rol y estado), mostrando rol y estado
    @Transactional(readOnly = true)
    public List<FilaUsuario> listar(String busqueda, Rol rol, Boolean activo) {
        String q = (busqueda == null) ? "" : busqueda.trim().toLowerCase();
        return usuarioRepository.findAll(Sort.by("nombre").ascending()).stream()
                .filter(u -> q.isEmpty()
                        || u.getNombre().toLowerCase().contains(q)
                        || u.getEmail().toLowerCase().contains(q))
                .filter(u -> rol == null || u.getRol() == rol)
                .filter(u -> activo == null || u.isActivo() == activo)
                .map(u -> new FilaUsuario(u.getId(), u.getNombre(), u.getEmail(),
                        u.getTelefono() == null ? "" : u.getTelefono(), u.getRol().name(), u.isActivo()))
                .toList();
    }

    // REQ-47, REQ-50: cambia el rol entre administrador y usuario regular (no deja el sistema sin administrador)
    @Transactional
    public void cambiarRol(Long id, Rol nuevoRol) {
        Usuario u = obtener(id);
        if (u.getRol() == nuevoRol) return;
        if (u.getRol() == Rol.ADMIN && u.isActivo()) {
            exigirOtroAdminActivo(u, "cambiar el rol de");
        }
        u.setRol(nuevoRol);
        usuarioRepository.save(u);
    }

    // REQ-48, REQ-50: activa o desactiva una cuenta (el historial de reservas se conserva; no se borra nada)
    @Transactional
    public void cambiarActivo(Long id, boolean activar) {
        Usuario u = obtener(id);
        if (u.isActivo() == activar) return;
        if (!activar && u.getRol() == Rol.ADMIN) {
            exigirOtroAdminActivo(u, "desactivar la cuenta de");
        }
        u.setActivo(activar);
        usuarioRepository.save(u);
    }

    // REQ-49: consulta las reservas de cualquier usuario
    @Transactional(readOnly = true)
    public ReservasDeUsuario reservasDe(Long usuarioId) {
        Usuario u = obtener(usuarioId);
        List<FilaReserva> filas = reservaRepository.findByUsuarioIdOrderByInicioDesc(usuarioId).stream()
                .map(this::aFila)
                .toList();
        return new ReservasDeUsuario(u.getId(), u.getNombre(), filas);
    }

    // REQ-49: el administrador cancela la reserva de otro usuario y se le notifica; devuelve el id del dueño
    @Transactional
    public Long cancelarReserva(Long reservaId) {
        Reserva r = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ReglaNegocioException("La reserva no existe."));
        if (!esCancelable(r)) {
            throw new ReglaNegocioException("Solo se pueden cancelar reservas RESERVADA o CONFIRMADA (esta está "
                    + r.getEstado() + ").");
        }
        r.setEstado(EstadoReserva.CANCELADA); // al salir de ACTIVAS el horario queda libre
        reservaRepository.save(r);
        notificacionService.notificar(r, TipoNotificacion.CANCELACION,
                "Un administrador canceló tu reserva en " + descripcion(r) + ".");
        return r.getUsuario().getId();
    }

    // REQ-50: si es el único administrador activo, rechaza la acción con un mensaje explicativo
    private void exigirOtroAdminActivo(Usuario u, String accion) {
        if (usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN) <= MINIMO_ADMINS_ACTIVOS) {
            throw new ReglaNegocioException("No se puede " + accion + " «" + u.getNombre()
                    + "»: es la única cuenta de administrador activa. "
                    + "Asigna primero el rol de administrador a otra cuenta activa.");
        }
    }

    // REQ-47, REQ-48: busca el usuario o avisa que no existe
    private Usuario obtener(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ReglaNegocioException("El usuario no existe."));
    }

    // REQ-49: solo RESERVADA y CONFIRMADA se pueden cancelar
    private boolean esCancelable(Reserva r) {
        return r.getEstado() == EstadoReserva.RESERVADA || r.getEstado() == EstadoReserva.CONFIRMADA;
    }

    // REQ-49: convierte una reserva en una fila de texto
    private FilaReserva aFila(Reserva r) {
        String estado = r.getEstado().name();
        return new FilaReserva(r.getId(), r.getEspacio().getNombre(), FECHA.format(r.getInicio()),
                HORA.format(r.getInicio()) + " – " + HORA.format(r.getFin()),
                estado, estado.replace('_', ' '), esCancelable(r));
    }

    // REQ-49: texto de la reserva para la notificación (espacio, fecha y horario)
    private String descripcion(Reserva r) {
        return "«" + r.getEspacio().getNombre() + "» el " + FECHA.format(r.getInicio())
                + " de " + HORA.format(r.getInicio()) + " a " + HORA.format(r.getFin());
    }
}