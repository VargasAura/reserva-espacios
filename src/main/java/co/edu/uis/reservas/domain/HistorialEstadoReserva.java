package co.edu.uis.reservas.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Registro de un cambio de estado de una reserva: cuándo ocurrió y quién (persona o proceso) lo hizo (REQ-27). */
@Entity
@Table(name = "historial_estado_reserva")
public class HistorialEstadoReserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;

    /** null cuando la reserva se acaba de crear. */
    @Enumerated(EnumType.STRING)
    private EstadoReserva estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReserva estadoNuevo;

    @Column(nullable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();

    /** Correo del usuario o administrador, o "SISTEMA" cuando lo hace una tarea automática. */
    @Column(nullable = false)
    private String responsable;

    public HistorialEstadoReserva() {}

    public HistorialEstadoReserva(Reserva reserva, EstadoReserva estadoAnterior, EstadoReserva estadoNuevo, String responsable) {
        this.reserva = reserva;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.responsable = responsable;
    }

    public Long getId() { return id; }
    public Reserva getReserva() { return reserva; }
    public void setReserva(Reserva reserva) { this.reserva = reserva; }
    public EstadoReserva getEstadoAnterior() { return estadoAnterior; }
    public void setEstadoAnterior(EstadoReserva estadoAnterior) { this.estadoAnterior = estadoAnterior; }
    public EstadoReserva getEstadoNuevo() { return estadoNuevo; }
    public void setEstadoNuevo(EstadoReserva estadoNuevo) { this.estadoNuevo = estadoNuevo; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public String getResponsable() { return responsable; }
    public void setResponsable(String responsable) { this.responsable = responsable; }
}