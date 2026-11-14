package co.edu.uis.reservas.dto;

import co.edu.uis.reservas.domain.Espacio;
import co.edu.uis.reservas.domain.TipoEspacio;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos del formulario de espacios (REQ-40, REQ-41, REQ-42). La tarifa no va aquí: queda vacía por ahora. */
public class EspacioForm {

    public static final int NOMBRE_MAX = 100;
    public static final int UBICACION_MAX = 150;
    public static final int CAPACIDAD_MINIMA = 1;

    /** null = espacio nuevo; con valor = editando ese espacio. */
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = NOMBRE_MAX, message = "El nombre es demasiado largo")
    private String nombre;

    @NotNull(message = "Selecciona el tipo de espacio")
    private TipoEspacio tipo;

    @NotNull(message = "La capacidad es obligatoria")
    @Min(value = CAPACIDAD_MINIMA, message = "La capacidad debe ser un número entero mayor que cero")
    private Integer capacidad;

    @NotBlank(message = "La ubicación es obligatoria")
    @Size(max = UBICACION_MAX, message = "La ubicación es demasiado larga")
    private String ubicacion;

    /** Llena el formulario con los datos de un espacio existente (para editar). */
    public static EspacioForm desde(Espacio e) {
        EspacioForm f = new EspacioForm();
        f.id = e.getId();
        f.nombre = e.getNombre();
        f.tipo = e.getTipo();
        f.capacidad = e.getCapacidad();
        f.ubicacion = e.getUbicacion();
        return f;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public TipoEspacio getTipo() { return tipo; }
    public void setTipo(TipoEspacio tipo) { this.tipo = tipo; }
    public Integer getCapacidad() { return capacidad; }
    public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }
    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }
}