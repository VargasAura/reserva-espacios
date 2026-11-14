package co.edu.uis.reservas.exception;

/** Se lanza cuando se viola una regla del SRS (conflicto de horario, datos inválidos, etc.). El mensaje se muestra al usuario. */
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) { super(mensaje); }
}
