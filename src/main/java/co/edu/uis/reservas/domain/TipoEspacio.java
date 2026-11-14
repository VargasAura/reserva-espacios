package co.edu.uis.reservas.domain;

public enum TipoEspacio {
    SALA_REUNIONES("Sala de reuniones"),
    AUDITORIO("Auditorio"),
    CANCHA_DEPORTIVA("Cancha deportiva"),
    AREA_RECREATIVA("Área recreativa"),
    OTRO("Otro");

    private final String etiqueta;
    TipoEspacio(String etiqueta) { this.etiqueta = etiqueta; }
    public String getEtiqueta() { return etiqueta; }
}
