package co.edu.uis.reservas.service;

import co.edu.uis.reservas.domain.CanalNotificacion;
import co.edu.uis.reservas.domain.Reserva;
import co.edu.uis.reservas.domain.Usuario;
import co.edu.uis.reservas.dto.PerfilForm;
import co.edu.uis.reservas.exception.ReglaNegocioException;
import co.edu.uis.reservas.repository.ReservaRepository;
import co.edu.uis.reservas.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PerfilService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    /** notificaciones = SMS, CORREO o NINGUNA (lo que se marca en el perfil). */
    public record DatosPerfil(String nombre, String email, String telefono, String notificaciones) {}

    /** Una fila del historial; estado = nombre del enum (para el color), estadoTexto = texto legible. */
    public record FilaHistorial(String espacio, String fecha, String horario, String estado, String estadoTexto) {}

    private final UsuarioRepository usuarioRepository;
    private final ReservaRepository reservaRepository;
    private final PasswordEncoder passwordEncoder;
    private final SmsService smsService;

    public PerfilService(UsuarioRepository usuarioRepository, ReservaRepository reservaRepository,
                         PasswordEncoder passwordEncoder, SmsService smsService) {
        this.smsService = smsService;
        this.usuarioRepository = usuarioRepository;
        this.reservaRepository = reservaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // REQ-8: nombre, correo y teléfono del usuario con sesión
    @Transactional(readOnly = true)
    public DatosPerfil datos(String email) {
        Usuario u = buscar(email);
        String notificaciones = u.isNotificacionesActivas() ? u.getCanalNotificacion().name() : "NINGUNA";
        return new DatosPerfil(u.getNombre(), u.getEmail(), u.getTelefono(), notificaciones);
    }

    // REQ-8: historial de reservas con espacio, fecha, horario y estado (más recientes primero)
    @Transactional(readOnly = true)
    public List<FilaHistorial> historial(String email) {
        Usuario u = buscar(email);
        return reservaRepository.findByUsuarioIdOrderByInicioDesc(u.getId()).stream()
                .map(this::aFila)
                .toList();
    }

    // REQ-9: actualiza nombre y teléfono; cambia la contraseña (cifrada) solo si la pidió y la actual es correcta
    @Transactional
    public void actualizar(String email, PerfilForm form) {
        Usuario u = buscar(email);

        boolean quiereCambiarPassword = hayTexto(form.getPasswordActual())
                || hayTexto(form.getPasswordNueva())
                || hayTexto(form.getPasswordConfirmacion());
        if (quiereCambiarPassword) {
            if (!hayTexto(form.getPasswordNueva())) {
                throw new ReglaNegocioException("Escribe la nueva contraseña.");
            }
            String actual = (form.getPasswordActual() == null) ? "" : form.getPasswordActual();
            if (!passwordEncoder.matches(actual, u.getPassword())) {
                throw new ReglaNegocioException("La contraseña actual no es correcta.");
            }
            if (!form.getPasswordNueva().equals(form.getPasswordConfirmacion())) {
                throw new ReglaNegocioException("La confirmación no coincide con la nueva contraseña.");
            }
            u.setPassword(passwordEncoder.encode(form.getPasswordNueva()));
        }

        aplicarNotificaciones(u, form);
        u.setNombre(form.getNombre().trim());
        u.setTelefono(form.getTelefono().trim());
        usuarioRepository.save(u);
    }

    // Preferencia de avisos: NINGUNA los desactiva (y recuerda el canal anterior); SMS exige un teléfono válido
    private void aplicarNotificaciones(Usuario u, PerfilForm form) {
        switch (form.getNotificaciones()) {
            case "NINGUNA" -> u.setNotificacionesActivas(false);
            case "CORREO" -> {
                u.setNotificacionesActivas(true);
                u.setCanalNotificacion(CanalNotificacion.CORREO);
            }
            default -> {
                if (smsService.normalizar(form.getTelefono()).isEmpty()) {
                    throw new ReglaNegocioException("Para recibir mensajes de texto necesitas un teléfono válido.");
                }
                u.setNotificacionesActivas(true);
                u.setCanalNotificacion(CanalNotificacion.SMS);
            }
        }
    }

    // REQ-8: convierte una reserva en una fila de texto lista para mostrar
    private FilaHistorial aFila(Reserva r) {
        String estado = r.getEstado().name();
        return new FilaHistorial(
                r.getEspacio().getNombre(),
                FECHA.format(r.getInicio()),
                HORA.format(r.getInicio()) + " – " + HORA.format(r.getFin()),
                estado,
                estado.replace('_', ' '));
    }

    // REQ-8, REQ-9: busca al usuario por el correo de su sesión
    private Usuario buscar(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ReglaNegocioException("Usuario no encontrado."));
    }

    // REQ-9: true si el texto no es nulo ni está en blanco
    private boolean hayTexto(String s) {
        return s != null && !s.isBlank();
    }
}