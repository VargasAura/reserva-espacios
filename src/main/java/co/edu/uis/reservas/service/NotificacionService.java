package co.edu.uis.reservas.service;

import co.edu.uis.reservas.config.ReglasNegocio;
import co.edu.uis.reservas.domain.CanalNotificacion;
import co.edu.uis.reservas.domain.Notificacion;
import co.edu.uis.reservas.domain.Reserva;
import co.edu.uis.reservas.domain.TipoNotificacion;
import co.edu.uis.reservas.domain.Usuario;
import co.edu.uis.reservas.repository.NotificacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

/**
 * Envío de notificaciones (REQ-33 a REQ-39).
 * Modo "simulado" (por defecto): escribe el mensaje en el log y lo da por enviado.
 * Modo "real": envía correo por SMTP con JavaMailSender (configurado en application.yml / variables de entorno).
 * Canal: el que la persona eligió en su perfil (SMS o correo). Si eligió SMS pero no tiene un teléfono válido,
 * se usa correo. Si desactivó las notificaciones, no se envía nada.
 * Un fallo de envío NUNCA lanza excepción ni revierte la reserva que lo originó (REQ-39).
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_MENSAJE = 1000; // largo de la columna "mensaje"

    private final NotificacionRepository repo;
    private final SmsService smsService;
    private final ObjectProvider<JavaMailSender> mailSender; // opcional: el sistema arranca sin servidor de correo
    private final String modo;
    private final String remitente;

    public NotificacionService(NotificacionRepository repo,
                               SmsService smsService,
                               ObjectProvider<JavaMailSender> mailSender,
                               @Value("${app.notificaciones.modo:simulado}") String modo,
                               @Value("${app.notificaciones.remitente:${spring.mail.username:no-reply@reservas.uis.edu.co}}") String remitente) {
        this.repo = repo;
        this.smsService = smsService;
        this.mailSender = mailSender;
        this.modo = modo;
        this.remitente = remitente;
    }

    // REQ-33, REQ-34, REQ-35, REQ-36, REQ-37, REQ-38, REQ-39
    @Transactional
    public Notificacion notificar(Reserva reserva, TipoNotificacion tipo, String mensaje) {
        Usuario usuario = reserva.getUsuario();
        if (!usuario.isNotificacionesActivas()) {
            log.info("Notificación de la reserva {} no enviada: la persona desactivó las notificaciones.", reserva.getId());
            return null; // nada se envía ni se guarda
        }
        CanalNotificacion canal = elegirCanal(usuario);
        String texto = construirMensaje(reserva, tipo, mensaje);
        boolean enviada = enviarConReintentos(usuario, canal, tipo, texto, mensaje, reserva.getId());

        Notificacion n = new Notificacion();
        n.setReserva(reserva);
        n.setTipo(tipo);
        n.setCanal(canal);
        n.setMensaje(texto.length() > MAX_MENSAJE ? texto.substring(0, MAX_MENSAJE - 3) + "..." : texto);
        n.setEnviada(enviada);
        return repo.save(n); // REQ-38: siempre se guarda, con resultado enviada true/false
    }

    // REQ-36: el canal que eligió la persona; SMS solo si además tiene un teléfono válido, si no se usa correo
    private CanalNotificacion elegirCanal(Usuario usuario) {
        boolean quiereSms = usuario.getCanalNotificacion() == CanalNotificacion.SMS;
        return (quiereSms && smsService.normalizar(usuario.getTelefono()).isPresent())
                ? CanalNotificacion.SMS : CanalNotificacion.CORREO;
    }

    // REQ-37: tipo + mensaje descriptivo + datos de la reserva
    private String construirMensaje(Reserva r, TipoNotificacion tipo, String mensaje) {
        return "[" + tipo.name().replace('_', ' ') + "] " + mensaje
                + "\nReserva #" + r.getId()
                + " · Espacio: " + r.getEspacio().getNombre()
                + " · Fecha: " + FECHA.format(r.getInicio())
                + " · Horario: " + HORA.format(r.getInicio()) + " a " + HORA.format(r.getFin())
                + " · Estado: " + r.getEstado().name().replace('_', ' ');
    }

    // REQ-39: hasta 3 intentos; si todos fallan se registra el fallo y se devuelve false (sin excepción)
    private boolean enviarConReintentos(Usuario usuario, CanalNotificacion canal, TipoNotificacion tipo,
                                        String texto, String mensajeCorto, Long reservaId) {
        for (int intento = 1; intento <= ReglasNegocio.REINTENTOS_NOTIFICACION; intento++) {
            try {
                enviar(usuario, canal, tipo, texto, mensajeCorto);
                return true;
            } catch (Exception e) {
                log.warn("Fallo al enviar notificación de la reserva {} (intento {}/{}): {}",
                        reservaId, intento, ReglasNegocio.REINTENTOS_NOTIFICACION, e.getMessage());
            }
        }
        log.error("No se pudo enviar la notificación de la reserva {} tras {} intentos.",
                reservaId, ReglasNegocio.REINTENTOS_NOTIFICACION);
        return false;
    }

    // REQ-36: envía por el canal elegido
    private void enviar(Usuario usuario, CanalNotificacion canal, TipoNotificacion tipo, String texto, String mensajeCorto) {
        if (canal == CanalNotificacion.SMS) {
            // El SMS lleva solo el mensaje (ya trae espacio, fecha y horario): corto para no gastar varios segmentos.
            smsService.enviar(usuario.getTelefono(), "Reservas UIS: " + mensajeCorto);
            return;
        }
        if (!"real".equalsIgnoreCase(modo)) {
            log.info("[CORREO simulado] a {}: {}", usuario.getEmail(), texto);
            return;
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            throw new IllegalStateException("No hay servidor de correo configurado (spring.mail.*).");
        }
        SimpleMailMessage correo = new SimpleMailMessage();
        correo.setFrom(remitente);
        correo.setTo(usuario.getEmail());
        correo.setSubject("Reserva de Espacios · " + tipo.name().replace('_', ' '));
        correo.setText(texto);
        sender.send(correo);
    }
}