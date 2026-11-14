package co.edu.uis.reservas.controller;

import co.edu.uis.reservas.dto.PerfilForm;
import co.edu.uis.reservas.exception.ReglaNegocioException;
import co.edu.uis.reservas.service.PerfilService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/** Perfil del usuario con sesión (REQ-8, REQ-9). */
@Controller
@RequestMapping("/perfil")
public class PerfilController {

    private static final String VISTA = "perfil/perfil";

    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @GetMapping
    public String ver(Principal principal, Model model) {
        PerfilService.DatosPerfil datos = perfilService.datos(principal.getName());
        PerfilForm form = new PerfilForm();
        form.setNombre(datos.nombre());
        form.setTelefono(datos.telefono());
        model.addAttribute("perfilForm", form);
        cargarDatos(model, principal.getName());
        return VISTA;
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("perfilForm") PerfilForm form, BindingResult result,
                          Principal principal, Model model, RedirectAttributes flash) {
        if (result.hasErrors()) {
            cargarDatos(model, principal.getName());
            return VISTA;
        }
        try {
            perfilService.actualizar(principal.getName(), form);
            flash.addFlashAttribute("exito", "Tus datos se actualizaron correctamente.");
            return "redirect:/perfil";
        } catch (ReglaNegocioException e) {
            result.reject("regla", e.getMessage());
            cargarDatos(model, principal.getName());
            return VISTA;
        }
    }

    /** Correo e historial que muestra la página (se vuelven a cargar si el formulario tiene errores). */
    private void cargarDatos(Model model, String email) {
        model.addAttribute("email", email);
        model.addAttribute("historial", perfilService.historial(email));
    }
}