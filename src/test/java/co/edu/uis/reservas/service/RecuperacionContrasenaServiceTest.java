package co.edu.uis.reservas.service;

import co.edu.uis.reservas.domain.TokenRecuperacion;
import co.edu.uis.reservas.domain.Usuario;
import co.edu.uis.reservas.exception.ReglaNegocioException;
import co.edu.uis.reservas.repository.TokenRecuperacionRepository;
import co.edu.uis.reservas.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RecuperacionContrasenaServiceTest {

    private UsuarioRepository usuarios;
    private TokenRecuperacionRepository tokens;
    private PasswordEncoder encoder;
    private JavaMailSender sender;
    private Usuario ana;

    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepository.class);
        tokens = mock(TokenRecuperacionRepository.class);
        encoder = mock(PasswordEncoder.class);
        sender = mock(JavaMailSender.class);
        ana = new Usuario();
        ana.setNombre("Ana");
        ana.setEmail("ana@test.co");
        ana.setPassword("VIEJA");
        when(usuarios.findByEmailIgnoreCase("ana@test.co")).thenReturn(Optional.of(ana));
    }

    @SuppressWarnings("unchecked")
    private RecuperacionContrasenaService servicio(String modo) {
        ObjectProvider<JavaMailSender> proveedor = mock(ObjectProvider.class);
        when(proveedor.getIfAvailable()).thenReturn(sender);
        return new RecuperacionContrasenaService(usuarios, tokens, encoder, proveedor, modo,
                "no-reply@test.co", "http://app.test/");
    }

    @Test
    void correoNoRegistradoDevuelveFalseYNoEnvia() {
        assertFalse(servicio("real").solicitar("nadie@test.co"));
        verify(tokens, never()).save(any());
        verify(sender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void cuentaInactivaNoRecibeEnlace() {
        ana.setActivo(false);
        assertFalse(servicio("real").solicitar("ana@test.co"));
        verify(tokens, never()).save(any());
    }

    @Test
    void guardaSoloElHashYEnviaElEnlaceEnElCorreo() {
        assertTrue(servicio("real").solicitar("ana@test.co"));

        ArgumentCaptor<TokenRecuperacion> guardado = ArgumentCaptor.forClass(TokenRecuperacion.class);
        verify(tokens).save(guardado.capture());
        ArgumentCaptor<SimpleMailMessage> correo = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(correo.capture());

        String texto = correo.getValue().getText();
        String token = texto.substring(texto.indexOf("token=") + 6).split("\\s")[0];
        assertTrue(texto.contains("http://app.test/restablecer?token="));
        assertEquals(RecuperacionContrasenaService.hash(token), guardado.getValue().getTokenHash());
        assertNotEquals(token, guardado.getValue().getTokenHash()); // el token en claro no se guarda
        assertEquals("ana@test.co", correo.getValue().getTo()[0]);
        assertTrue(guardado.getValue().getExpiraEn().isAfter(LocalDateTime.now().plusMinutes(59)));
    }

    @Test
    void enModoSimuladoNoEnviaCorreo() {
        servicio("simulado").solicitar("ana@test.co");
        verify(tokens).save(any());
        verify(sender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void siFallaElCorreoNoLanzaError() {
        doThrow(new RuntimeException("sin conexión")).when(sender).send(any(SimpleMailMessage.class));
        assertDoesNotThrow(() -> servicio("real").solicitar("ana@test.co"));
    }

    @Test
    void solicitudRepetidaMuyProntoSeIgnora() {
        when(tokens.findTopByUsuarioIdOrderByCreadoEnDesc(any()))
                .thenReturn(Optional.of(new TokenRecuperacion(ana, "x", LocalDateTime.now().plusHours(1))));
        servicio("real").solicitar("ana@test.co");
        verify(tokens, never()).save(any());
    }

    @Test
    void conEnlaceValidoCambiaLaContrasenaYGastaElEnlace() {
        when(tokens.findByTokenHash(RecuperacionContrasenaService.hash("abc")))
                .thenReturn(Optional.of(new TokenRecuperacion(ana, "h", LocalDateTime.now().plusMinutes(30))));
        when(encoder.encode("Nueva1234")).thenReturn("CIFRADA");

        servicio("simulado").restablecer("abc", "Nueva1234");

        assertEquals("CIFRADA", ana.getPassword());
        verify(usuarios).save(ana);
        verify(tokens).deleteByUsuarioId(any());
    }

    @Test
    void enlaceVencidoSeRechaza() {
        when(tokens.findByTokenHash(RecuperacionContrasenaService.hash("abc")))
                .thenReturn(Optional.of(new TokenRecuperacion(ana, "h", LocalDateTime.now().minusMinutes(1))));
        RecuperacionContrasenaService s = servicio("simulado");
        assertFalse(s.tokenValido("abc"));
        assertThrows(ReglaNegocioException.class, () -> s.restablecer("abc", "Nueva1234"));
        assertEquals("VIEJA", ana.getPassword());
    }

    @Test
    void enlaceInexistenteOVacioSeRechaza() {
        RecuperacionContrasenaService s = servicio("simulado");
        assertFalse(s.tokenValido("zzz"));
        assertFalse(s.tokenValido(null));
        assertFalse(s.tokenValido("  "));
        assertThrows(ReglaNegocioException.class, () -> s.restablecer("zzz", "Nueva1234"));
    }
}
