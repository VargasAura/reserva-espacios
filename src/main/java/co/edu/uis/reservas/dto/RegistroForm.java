package co.edu.uis.reservas.dto;

import jakarta.validation.constraints.*;

/** Datos del formulario de registro (REQ-1, REQ-2). */
public class RegistroForm {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
             message = "Mínimo 8 caracteres, con al menos una letra y un número")
    private String password;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[0-9+\\s-]{7,15}$", message = "El teléfono no es válido")
    private String telefono;

    @AssertTrue(message = "Debes aceptar la política de tratamiento de datos personales")
    private boolean aceptaPolitica;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public boolean isAceptaPolitica() { return aceptaPolitica; }
    public void setAceptaPolitica(boolean aceptaPolitica) { this.aceptaPolitica = aceptaPolitica; }
}
