package co.edu.uis.reservas.controller;

import co.edu.uis.reservas.domain.Espacio;
import co.edu.uis.reservas.domain.TipoEspacio;
import co.edu.uis.reservas.dto.EspacioForm;
import co.edu.uis.reservas.exception.ReglaNegocioException;
import co.edu.uis.reservas.service.EspacioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** CRUD de espacios (REQ-40 a REQ-45). REQ-45: /admin/** ya es solo ADMIN en SecurityConfig. */
@Controller
@RequestMapping("/admin/espacios")
public class AdminEspacioController {

    private static final String VISTA_LISTA = "admin/espacios-lista";
    private static final String VISTA_FORM = "admin/espacio-form";
    private static final String REDIRIGIR_LISTA = "redirect:/admin/espacios";

    private final EspacioService espacioService;

    public AdminEspacioController(EspacioService espacioService) {
        this.espacioService = espacioService;
    }

    /** Opciones del desplegable "Tipo" en el formulario. */
    @ModelAttribute("tipos")
    public TipoEspacio[] tipos() { return TipoEspacio.values(); }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("espacios", espacioService.listarActivos());
        return VISTA_LISTA;
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("espacioForm", new EspacioForm());
        return VISTA_FORM;
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("espacioForm") EspacioForm form, BindingResult result,
                        Model model, RedirectAttributes flash) {
        form.setId(null);
        if (tieneErrores(result, model)) return VISTA_FORM;
        try {
            Espacio creado = espacioService.crear(form);
            flash.addFlashAttribute("exito", "Espacio «" + creado.getNombre() + "» creado correctamente.");
            return REDIRIGIR_LISTA;
        } catch (ReglaNegocioException e) {
            result.reject("regla", e.getMessage());
            return VISTA_FORM;
        }
    }

    @GetMapping("/{id}/editar")
    public String editarForm(@PathVariable Long id, Model model, RedirectAttributes flash) {
        try {
            model.addAttribute("espacioForm", EspacioForm.desde(espacioService.obtenerActivo(id)));
            return VISTA_FORM;
        } catch (ReglaNegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return REDIRIGIR_LISTA;
        }
    }

    @PostMapping("/{id}")
    public String editar(@PathVariable Long id, @Valid @ModelAttribute("espacioForm") EspacioForm form,
                         BindingResult result, Model model, RedirectAttributes flash) {
        form.setId(id);
        if (tieneErrores(result, model)) return VISTA_FORM;
        try {
            Espacio editado = espacioService.editar(id, form);
            flash.addFlashAttribute("exito", "Espacio «" + editado.getNombre() + "» actualizado correctamente.");
            return REDIRIGIR_LISTA;
        } catch (ReglaNegocioException e) {
            result.reject("regla", e.getMessage());
            return VISTA_FORM;
        }
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes flash) {
        try {
            espacioService.eliminar(id);
            flash.addFlashAttribute("exito", "Espacio eliminado. Su historial de reservas se conserva.");
        } catch (ReglaNegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return REDIRIGIR_LISTA;
    }

    /** Si escriben letras en "capacidad", Spring daría un mensaje en inglés; marcamos el caso para mostrar uno en español. */
    private boolean tieneErrores(BindingResult result, Model model) {
        FieldError capacidad = result.getFieldError("capacidad");
        if (capacidad != null && capacidad.isBindingFailure()) {
            model.addAttribute("capacidadNoNumerica", true);
        }
        return result.hasErrors();
    }
}