package co.edu.uis.reservas.service;

import co.edu.uis.reservas.config.Telefonos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

/**
 * Envío de mensajes de texto (SMS) al teléfono de la persona.
 * Modo "simulado" (por defecto): solo escribe el mensaje en el log y lo da por enviado.
 * Modo "real": lo envía con Twilio (API REST, sin librerías extra). Se configura con variables de entorno
 * (ver application.yml). Cualquier fallo lanza excepción; NotificacionService la atrapa y reintenta (REQ-39).
 */
@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);
    private static final Duration TIEMPO_CONEXION = Duration.ofSeconds(5);
    private static final Duration TIEMPO_RESPUESTA = Duration.ofSeconds(10);

    private final String modo;
    private final String codigoPais;
    private final String accountSid;
    private final String authToken;
    private final String desde;
    private final HttpClient http;

    public SmsService(@Value("${app.sms.modo:simulado}") String modo,
                      @Value("${app.sms.codigo-pais:+57}") String codigoPais,
                      @Value("${app.sms.twilio.account-sid:}") String accountSid,
                      @Value("${app.sms.twilio.auth-token:}") String authToken,
                      @Value("${app.sms.twilio.desde:}") String desde) {
        this.modo = modo;
        this.codigoPais = codigoPais;
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.desde = desde;
        this.http = HttpClient.newBuilder().connectTimeout(TIEMPO_CONEXION).build();
    }

    /** Teléfono en formato internacional (+573001234567), o vacío si el número no sirve para enviar SMS. */
    public Optional<String> normalizar(String telefono) {
        return Telefonos.aE164(telefono, codigoPais);
    }

    /** Envía el SMS. Lanza excepción si el número no es válido o el proveedor no lo acepta. */
    public void enviar(String telefono, String texto) {
        String destino = normalizar(telefono)
                .orElseThrow(() -> new IllegalArgumentException("El teléfono registrado no es válido para SMS."));

        if (!"real".equalsIgnoreCase(modo)) {
            log.info("[SMS simulado] a {}: {}", destino, texto);
            return;
        }
        if (accountSid.isBlank() || authToken.isBlank() || desde.isBlank()) {
            throw new IllegalStateException(
                    "Faltan las credenciales de Twilio (TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_NUMERO).");
        }
        enviarPorTwilio(destino, texto);
    }

    private void enviarPorTwilio(String destino, String texto) {
        String cuerpo = "To=" + codificar(destino) + "&From=" + codificar(desde) + "&Body=" + codificar(texto);
        String credenciales = Base64.getEncoder()
                .encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));
        HttpRequest peticion = HttpRequest.newBuilder(
                        URI.create("https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json"))
                .timeout(TIEMPO_RESPUESTA)
                .header("Authorization", "Basic " + credenciales)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                .build();
        try {
            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() / 100 != 2) {
                throw new IllegalStateException("Twilio rechazó el SMS (HTTP " + respuesta.statusCode() + ").");
            }
            log.info("[SMS real] enviado a {}", destino);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo conectar con Twilio: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("El envío del SMS fue interrumpido.", e);
        }
    }

    private static String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }
}
