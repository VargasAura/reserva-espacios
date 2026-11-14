package co.edu.uis.reservas.controller;

import co.edu.uis.reservas.domain.TipoEspacio;
import co.edu.uis.reservas.service.CalendarioService;
import co.edu.uis.reservas.service.CalendarioService.Disponibilidad;
import co.edu.uis.reservas.service.CalendarioService.Franja;
import co.edu.uis.reservas.service.CalendarioService.Vista;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Pantalla del calendario y su JSON de datos (REQ-10 a REQ-14). */
@Controller
@RequestMapping("/calendario")
public class CalendarioController {

    private final CalendarioService calendarioService;

    public CalendarioController(CalendarioService calendarioService) {
        this.calendarioService = calendarioService;
    }

    // REQ-10, REQ-11: página con los filtros (vista, tipo de espacio y franja horaria)
    @GetMapping
    public String pagina(Model model) {
        model.addAttribute("vistas", Vista.values());
        model.addAttribute("tipos", TipoEspacio.values());
        model.addAttribute("franjas", Franja.values());
        return "calendario/calendario";
    }

    // REQ-10, REQ-11, REQ-12, REQ-14: datos en JSON; el JavaScript los pide cada pocos segundos.
    // Cualquier valor inválido o vacío se cambia por el valor por defecto (nunca da error 500).
    @GetMapping("/datos")
    @ResponseBody
    public Disponibilidad datos(@RequestParam(required = false) String vista,
                                @RequestParam(required = false) String fecha,
                                @RequestParam(required = false) String tipo,
                                @RequestParam(required = false) String franja) {
        return calendarioService.consultar(
                enumONulo(Vista.class, vista, Vista.DIA),
                fechaONulo(fecha),
                enumONulo(TipoEspacio.class, tipo, null),
                enumONulo(Franja.class, franja, Franja.DIA_COMPLETO));
    }

    // REQ-11: convierte el texto en un valor del enum; si es inválido devuelve el valor por defecto
    private static <E extends Enum<E>> E enumONulo(Class<E> clase, String texto, E porDefecto) {
        if (texto == null || texto.isBlank()) return porDefecto;
        try {
            return Enum.valueOf(clase, texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return porDefecto;
        }
    }

    // REQ-11: convierte "AAAA-MM-DD" en fecha; si es inválida usa hoy
    private static LocalDate fechaONulo(String texto) {
        if (texto == null || texto.isBlank()) return LocalDate.now();
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException e) {
            return LocalDate.now();
        }
    }
}