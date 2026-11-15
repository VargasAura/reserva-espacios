package co.edu.uis.reservas.service;

import co.edu.uis.reservas.domain.CanalNotificacion;
import co.edu.uis.reservas.domain.Espacio;
import co.edu.uis.reservas.domain.EstadoReserva;
import co.edu.uis.reservas.domain.Notificacion;
import co.edu.uis.reservas.domain.Reserva;
import co.edu.uis.reservas.domain.TipoNotificacion;
import co.edu.uis.reservas.domain.Usuario;
import co.edu.uis.reservas.repository.NotificacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificacionServiceTest {

    private NotificacionRepository repo;
    private ObjectProvider<JavaMailSender> proveedor;
    private JavaMailSender sender;
    private SmsService sms;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void preparar() {
        repo = mock(NotificacionRepository.class);
        when(repo.save(any(Notificacion.class))).thenAnswer(i -> i.getArgument(0));
        proveedor = mock(ObjectProvider.class);
        sender = mock(JavaMailSender.class);
        sms = mock(SmsService.class);
        // normalizar real simplificado: solo hay SMS si el teléfono tiene 10 dígitos
        when(sms.normalizar(any())).thenAnswer(i -> {
            String t = i.getArgument(0);
            return (t != null && t.trim().matches("\\d{10}")) ? Optional.of("+57" + t.trim()) : Optional.empty();
        });
    }

    private NotificacionService servicio(String modo) {
        return new NotificacionService(repo, sms, proveedor, modo, "no-reply@test.co");
    }

    private Usuario usuario(CanalNotificacion canal, String telefono) {
        Usuario u = new Usuario();
        u.setNombre("Ana");
        u.setEmail("ana@test.co");
        u.setTelefono(telefono);
        u.setCanalNotificacion(canal);
        return u;
    }

    private Reserva reserva(Usuario u) {
        Espacio espacio = mock(Espacio.class);
        when(espacio.getNombre()).thenReturn("Sala 1");
        Reserva r = mock(Reserva.class);
        when(r.getId()).thenReturn(7L);
        when(r.getUsuario()).thenReturn(u);
        when(r.getEspacio()).thenReturn(espacio);
        when(r.getInicio()).thenReturn(LocalDateTime.of(2026, 11, 20, 10, 0));
        when(r.getFin()).thenReturn(LocalDateTime.of(2026, 11, 20, 11, 0));
        when(r.getEstado()).thenReturn(EstadoReserva.CONFIRMADA);
        return r;
    }

    // REQ-37, REQ-38: modo simulado guarda la fila como enviada, con tipo y datos de la reserva
    @Test
    void simuladoGuardaFilaEnviada() {
        Notificacion n = servicio("simulado")
                .notificar(reserva(usuario(CanalNotificacion.CORREO, null)), TipoNotificacion.RECORDATORIO, "Hola");
        assertTrue(n.isEnviada());
        assertEquals(TipoNotificacion.RECORDATORIO, n.getTipo());
        assertTrue(n.getMensaje().contains("Sala 1"));
        verify(repo).save(any(Notificacion.class));
    }

    // REQ-36: eligió SMS pero no tiene teléfono válido: se usa correo
    @Test
    void sinTelefonoUsaCorreo() {
        Notificacion n = servicio("simulado")
                .notificar(reserva(usuario(CanalNotificacion.SMS, "  ")), TipoNotificacion.RECORDATORIO, "Hola");
        assertEquals(CanalNotificacion.CORREO, n.getCanal());
        verify(sms, never()).enviar(any(), any());
    }

    // Si eligió SMS y tiene teléfono válido, el aviso va por SMS con un mensaje corto
    @Test
    void conPreferenciaSmsEnviaMensajeCorto() {
        Notificacion n = servicio("simulado")
                .notificar(reserva(usuario(CanalNotificacion.SMS, "3001234567")), TipoNotificacion.CONFIRMACION,
                        "Tu reserva fue confirmada.");
        assertEquals(CanalNotificacion.SMS, n.getCanal());
        assertTrue(n.isEnviada());
        verify(sms).enviar("3001234567", "Reservas UIS: Tu reserva fue confirmada.");
        verify(sender, never()).send(any(SimpleMailMessage.class));
    }

    // Si eligió correo, no se manda SMS aunque tenga teléfono
    @Test
    void conPreferenciaCorreoNoEnviaSms() {
        Notificacion n = servicio("simulado")
                .notificar(reserva(usuario(CanalNotificacion.CORREO, "3001234567")), TipoNotificacion.CONFIRMACION, "Hola");
        assertEquals(CanalNotificacion.CORREO, n.getCanal());
        verify(sms, never()).enviar(any(), any());
    }

    // Si desactivó las notificaciones no se envía ni se guarda nada
    @Test
    void conNotificacionesDesactivadasNoEnviaNada() {
        Usuario u = usuario(CanalNotificacion.SMS, "3001234567");
        u.setNotificacionesActivas(false);
        Notificacion n = servicio("simulado").notificar(reserva(u), TipoNotificacion.CANCELACION, "Cancelada");
        assertNull(n);
        verify(sms, never()).enviar(any(), any());
        verify(repo, never()).save(any(Notificacion.class));
    }

    // REQ-39: si el SMS falla, reintenta 3 veces, guarda enviada=false y NO lanza excepción
    @Test
    void smsConFalloReintentaTresVeces() {
        doThrow(new IllegalStateException("sin conexión")).when(sms).enviar(any(), any());
        Notificacion n = assertDoesNotThrow(() -> servicio("simulado")
                .notificar(reserva(usuario(CanalNotificacion.SMS, "3001234567")), TipoNotificacion.CANCELACION, "Cancelada"));
        assertFalse(n.isEnviada());
        assertEquals(CanalNotificacion.SMS, n.getCanal());
        verify(sms, times(3)).enviar(any(), any());
    }

    // REQ-39: modo real con envío correcto manda un solo correo
    @Test
    void realEnviaCorreo() {
        when(proveedor.getIfAvailable()).thenReturn(sender);
        Notificacion n = servicio("real")
                .notificar(reserva(usuario(CanalNotificacion.CORREO, null)), TipoNotificacion.CANCELACION, "Cancelada");
        assertTrue(n.isEnviada());
        verify(sender, times(1)).send(any(SimpleMailMessage.class));
    }

    // REQ-39: si el correo falla, reintenta 3 veces, guarda enviada=false y NO lanza excepción
    @Test
    void realConFalloReintentaTresVeces() {
        when(proveedor.getIfAvailable()).thenReturn(sender);
        doThrow(new MailSendException("sin conexión")).when(sender).send(any(SimpleMailMessage.class));
        Notificacion n = assertDoesNotThrow(() -> servicio("real")
                .notificar(reserva(usuario(CanalNotificacion.CORREO, null)), TipoNotificacion.CANCELACION, "Cancelada"));
        assertFalse(n.isEnviada());
        verify(sender, times(3)).send(any(SimpleMailMessage.class));
        verify(repo).save(any(Notificacion.class));
    }

    // REQ-39: modo real sin servidor de correo configurado también queda como fallida, sin excepción
    @Test
    void realSinServidorQuedaFallida() {
        when(proveedor.getIfAvailable()).thenReturn(null);
        Notificacion n = assertDoesNotThrow(() -> servicio("real")
                .notificar(reserva(usuario(CanalNotificacion.CORREO, null)), TipoNotificacion.RECORDATORIO, "Hola"));
        assertFalse(n.isEnviada());
    }
}