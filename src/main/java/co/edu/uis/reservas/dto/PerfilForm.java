package co.edu.uis.reservas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Datos editables del perfil (REQ-9). Los campos de contraseña son opcionales: solo se usan si quiere cambiarla. */
public class PerfilForm {

    /** Misma regla de contraseña del registro; vacío también es válido (no cambiar contraseña). */
    public static final String PATRON_PASSWORD = "^$|^(?=.*[A-Za-z])(?=.*\\d).{8,}$";

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[0-9+\\s-]{7,15}$", message = "El teléfono no es válido")
    private String telefono;

    /** Cómo quiere recibir los avisos: SMS, CORREO o NINGUNA. */
    @NotBlank(message = "Elige cómo quieres recibir las notificaciones")
    @Pattern(regexp = "^(SMS|CORREO|NINGUNA)$", message = "Opción de notificaciones no válida")
    private String notificaciones;

    private String passwordActual;

    @Pattern(regexp = PATRON_PASSWORD, message = "Mínimo 8 caracteres, con al menos una letra y un número")
    private String passwordNueva;

    private String passwordConfirmacion;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getNotificaciones() { return notificaciones; }
    public void setNotificaciones(String notificaciones) { this.notificaciones = notificaciones; }
    public String getPasswordActual() { return passwordActual; }
    public void setPasswordActual(String passwordActual) { this.passwordActual = passwordActual; }
    public String getPasswordNueva() { return passwordNueva; }
    public void setPasswordNueva(String passwordNueva) { this.passwordNueva = passwordNueva; }
    public String getPasswordConfirmacion() { return passwordConfirmacion; }
    public void setPasswordConfirmacion(String passwordConfirmacion) { this.passwordConfirmacion = passwordConfirmacion; }
}