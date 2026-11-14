package co.edu.uis.reservas.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Espacio compartido reservable. 'activo = false' es la eliminación lógica (REQ-44). */
@Entity
@Table(name = "espacios")
public class Espacio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEspacio tipo;

    @Column(nullable = false)
    private int capacidad;

    @Column(nullable = false)
    private String ubicacion;

    /** Tarifa por hora. null = gratuito (REQ-57). */
    @Column(precision = 12, scale = 2)
    private BigDecimal tarifaHora;

    @Column(nullable = false)
    private boolean activo = true;

    public Espacio() {}

    public Espacio(String nombre, TipoEspacio tipo, int capacidad, String ubicacion) {
        this.nombre = nombre; this.tipo = tipo; this.capacidad = capacidad; this.ubicacion = ubicacion;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public TipoEspacio getTipo() { return tipo; }
    public void setTipo(TipoEspacio tipo) { this.tipo = tipo; }
    public int getCapacidad() { return capacidad; }
    public void setCapacidad(int capacidad) { this.capacidad = capacidad; }
    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }
    public BigDecimal getTarifaHora() { return tarifaHora; }
    public void setTarifaHora(BigDecimal tarifaHora) { this.tarifaHora = tarifaHora; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
