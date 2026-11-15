package co.edu.uis.reservas.controller;

import co.edu.uis.reservas.dto.OlvideForm;
import co.edu.uis.reservas.dto.RestablecerForm;
import co.edu.uis.reservas.exception.ReglaNegocioException;
import co.edu.uis.reservas.service.RecuperacionContrasenaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

/** Pantallas públicas de "Olvidé mi contraseña" y de elegir la contraseña nueva. */
@Controller
public class RecuperacionController {

    private final RecuperacionContrasenaService servicio;

    public RecuperacionController(RecuperacionContrasenaService servicio) { this.servicio = servicio; }

    @GetMapping("/olvide-contrasena")
    public String olvideForm(Model model) {
        model.addAttribute("olvideForm", new OlvideForm());
        return "olvide-contrasena";
    }

    // Primero se verifica que el correo esté asociado a una cuenta; si no, se avisa y no se envía nada
    @PostMapping("/olvide-contrasena")
    public String olvide(@Valid @ModelAttribute("olvideForm") OlvideForm form, BindingResult result, Model model) {
        if (result.hasErrors()) return "olvide-contrasena";
        if (!servicio.solicitar(form.getEmail())) {
            result.rejectValue("email", "noRegistrado", "Este correo no está asociado a ninguna cuenta.");
            return "olvide-contrasena";
        }
        model.addAttribute("enviado", true);
        model.addAttribute("correoEnviado", form.getEmail().trim());
        return "olvide-contrasena";
    }

    @GetMapping("/restablecer")
    public String restablecerForm(@RequestParam(required = false) String token, Model model) {
        RestablecerForm form = new RestablecerForm();
        form.setToken(token);
        model.addAttribute("restablecerForm", form);
        model.addAttribute("tokenValido", servicio.tokenValido(token));
        return "restablecer";
    }

    @PostMapping("/restablecer")
    public String restablecer(@Valid @ModelAttribute("restablecerForm") RestablecerForm form,
                              BindingResult result, Model model) {
        model.addAttribute("tokenValido", true);
        if (!result.hasFieldErrors("password") && !String.valueOf(form.getPassword()).equals(form.getConfirmacion())) {
            result.rejectValue("confirmacion", "noCoincide", "Las contraseñas no coinciden");
        }
        if (result.hasErrors()) return "restablecer";
        try {
            servicio.restablecer(form.getToken(), form.getPassword());
        } catch (ReglaNegocioException e) {
            model.addAttribute("tokenValido", false); // enlace vencido o ya usado
            return "restablecer";
        }
        return "redirect:/login?restablecida";
    }
}
