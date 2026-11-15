package co.edu.uis.reservas.config;

import java.util.Optional;

/** Convierte teléfonos escritos por las personas al formato internacional (E.164) que exigen los proveedores de SMS. */
public final class Telefonos {

    private Telefonos() {}

    /**
     * "300 123 4567" -> "+573001234567" (con código de país +57). Acepta "+57 300-123-4567", "573001234567" y "0057...".
     * Devuelve vacío si no se puede interpretar como un número válido.
     *
     * @param codigoPais código de país por defecto, con o sin "+", por ejemplo "+57"
     */
    public static Optional<String> aE164(String texto, String codigoPais) {
        if (texto == null) return Optional.empty();
        String t = texto.trim().replaceAll("[\\s\\-().]", "");
        if (t.isEmpty()) return Optional.empty();

        String cc = codigoPais == null ? "" : codigoPais.replace("+", "").trim();
        if (t.startsWith("00")) {
            t = "+" + t.substring(2);
        } else if (!t.startsWith("+")) {
            // ya trae el código de país escrito sin "+" (57 + 10 dígitos) o es un número nacional
            boolean yaTraeCodigo = !cc.isEmpty() && t.startsWith(cc) && t.length() >= cc.length() + 10;
            t = "+" + (yaTraeCodigo ? "" : cc) + t;
        }
        return t.matches("^\\+[1-9]\\d{7,14}$") ? Optional.of(t) : Optional.empty();
    }
}
