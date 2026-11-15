package co.edu.uis.reservas.dto;

import co.edu.uis.reservas.domain.Reserva;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Datos del formulario de reserva (REQ-15, REQ-21). Las reglas de horario (REQ-16) se revisan en ReservaService. */
public class ReservaForm {

    /** null = reserva nueva; con valor = editando esa reserva. */
    private Long id;

    @NotNull(message = "Selecciona un espacio")
    private Long espacioId;

    @NotNull(message = "La fecha es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecha;

    @NotNull(message = "La hora de inicio es obligatoria")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin es obligatoria")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime horaFin;

    /** Llena el formulario con una reserva existente (para editar). Llamar dentro del servicio. */
    public static ReservaForm desde(Reserva r) {
        ReservaForm f = new ReservaForm();
        f.id = r.getId();
        f.espacioId = r.getEspacio().getId();
        f.fecha = r.getInicio().toLocalDate();
        f.horaInicio = r.getInicio().toLocalTime();
        f.horaFin = r.getFin().toLocalTime();
        return f;
    }

    public LocalDateTime calcularInicio() { return LocalDateTime.of(fecha, horaInicio); }
    /** La hora de fin 00:00 significa la medianoche al terminar ese día (24:00), no el inicio del mismo día. */
    public LocalDateTime calcularFin() {
        return LocalTime.MIDNIGHT.equals(horaFin) ? fecha.plusDays(1).atStartOfDay() : LocalDateTime.of(fecha, horaFin);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEspacioId() { return espacioId; }
    public void setEspacioId(Long espacioId) { this.espacioId = espacioId; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }
}