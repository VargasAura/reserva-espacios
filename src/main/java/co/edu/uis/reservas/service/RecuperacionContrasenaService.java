package co.edu.uis.reservas.service;

import co.edu.uis.reservas.domain.TokenRecuperacion;
import co.edu.uis.reservas.domain.Usuario;
import co.edu.uis.reservas.exception.ReglaNegocioException;
import co.edu.uis.reservas.repository.TokenRecuperacionRepository;
import co.edu.uis.reservas.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * "Olvidé mi contraseña": envía por correo un enlace de un solo uso y válido por 1 hora.
 * - El token es aleatorio (256 bits); en la base de datos solo se guarda su hash.
 * - Primero se verifica que el correo esté asociado a una cuenta activa; si no, se avisa y no se envía nada.
 * - Modo "simulado" (por defecto): el enlace se escribe en el log en vez de enviarse. Modo "real": correo SMTP.
 * Este correo se envía siempre, aunque la persona haya desactivado las notificaciones de reservas.
 */
@Service
public class RecuperacionContrasenaService {

    private static final Logger log = LoggerFactory.getLogger(RecuperacionContrasenaService.class);
    public static final Duration VIGENCIA = Duration.ofHours(1);
    /** Espera mínima entre dos solicitudes de la misma cuenta (evita llenar el correo de alguien). */
    static final Duration ESPERA_ENTRE_SOLICITUDES = Duration.ofMinutes(1);
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final UsuarioRepository usuarios;
    private final TokenRecuperacionRepository tokens;
    private final PasswordEncoder encoder;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String modo;
    private final String remitente;
    private final String urlBase;

    public RecuperacionContrasenaService(UsuarioRepository usuarios, TokenRecuperacionRepository tokens,
                                         PasswordEncoder encoder, ObjectProvider<JavaMailSender> mailSender,
                                         @Value("${app.notificaciones.modo:simulado}") String modo,
                                         @Value("${app.notificaciones.remitente:no-reply@reservas.uis.edu.co}") String remitente,
                                         @Value("${app.url-base:http://localhost:8080}") String urlBase) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.encoder = encoder;
        this.mailSender = mailSender;
        this.modo = modo;
        this.remitente = remitente;
        this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
    }

    /**
     * Verifica que el correo sea de una cuenta activa; si lo es, crea el enlace y lo envía.
     * @return true si el correo está asociado a una cuenta (se envió el enlace o ya se había enviado hace instantes);
     *         false si no hay ninguna cuenta activa con ese correo (no se envía nada).
     */
    @Transactional
    public boolean solicitar(String email) {
        Optional<Usuario> encontrado = usuarios.findByEmailIgnoreCase(email == null ? "" : email.trim())
                .filter(Usuario::isActivo);
        if (encontrado.isEmpty()) {
            log.info("Recuperación de contraseña: el correo no corresponde a una cuenta activa.");
            return false;
        }
        Usuario usuario = encontrado.get();

        Optional<TokenRecuperacion> ultimo = tokens.findTopByUsuarioIdOrderByCreadoEnDesc(usuario.getId());
        if (ultimo.isPresent() && ultimo.get().getCreadoEn().plus(ESPERA_ENTRE_SOLICITUDES).isAfter(LocalDateTime.now())) {
            log.info("Recuperación de contraseña: solicitud repetida muy pronto, no se envía otro enlace.");
            return true; // la cuenta existe y ya se le envió un enlace hace menos de un minuto
        }

        tokens.deleteByUsuarioId(usuario.getId()); // solo vale el enlace más reciente
        String token = generarToken();
        tokens.save(new TokenRecuperacion(usuario, hash(token), LocalDateTime.now().plus(VIGENCIA)));
        enviarCorreo(usuario, urlBase + "/restablecer?token=" + token);
        return true;
    }

    /** true si el enlace existe, no venció y la cuenta sigue activa. */
    @Transactional(readOnly = true)
    public boolean tokenValido(String token) {
        return buscarVigente(token).isPresent();
    }

    /** Cambia la contraseña con un enlace válido y lo gasta (borra todos los enlaces de esa cuenta). */
    @Transactional
    public void restablecer(String token, String nuevaPassword) {
        TokenRecuperacion t = buscarVigente(token)
                .orElseThrow(() -> new ReglaNegocioException("El enlace no es válido o ya venció. Solicita uno nuevo."));
        Usuario usuario = t.getUsuario();
        usuario.setPassword(encoder.encode(nuevaPassword));
        usuarios.save(usuario);
        tokens.deleteByUsuarioId(usuario.getId());
    }

    private Optional<TokenRecuperacion> buscarVigente(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        return tokens.findByTokenHash(hash(token.trim()))
                .filter(t -> t.getExpiraEn().isAfter(LocalDateTime.now()))
                .filter(t -> t.getUsuario().isActivo());
    }

    // Un fallo al enviar el correo se registra en el log pero no se le muestra a la persona
    private void enviarCorreo(Usuario usuario, String enlace) {
        String texto = "Hola, " + usuario.getNombre() + ".\n\n"
                + "Recibimos una solicitud para restablecer la contraseña de tu cuenta en Reserva de Espacios.\n"
                + "Entra a este enlace para elegir una contraseña nueva (sirve una sola vez y vence en 1 hora):\n\n"
                + enlace + "\n\n"
                + "Si no fuiste tú, ignora este mensaje: tu contraseña actual no cambia.";
        try {
            if (!"real".equalsIgnoreCase(modo)) {
                log.info("[CORREO simulado] a {} · Recuperación de contraseña. Enlace: {}", usuario.getEmail(), enlace);
                return;
            }
            JavaMailSender sender = mailSender.getIfAvailable();
            if (sender == null) throw new IllegalStateException("No hay servidor de correo configurado (spring.mail.*).");
            SimpleMailMessage correo = new SimpleMailMessage();
            correo.setFrom(remitente);
            correo.setTo(usuario.getEmail());
            correo.setSubject("Reserva de Espacios · Restablecer contraseña");
            correo.setText(texto);
            sender.send(correo);
        } catch (Exception e) {
            log.error("No se pudo enviar el correo de recuperación: {}", e.getMessage());
        }
    }

    private static String generarToken() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 en hexadecimal (64 caracteres). */
    static String hash(String token) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e); // SHA-256 existe en toda JVM
        }
    }
}
