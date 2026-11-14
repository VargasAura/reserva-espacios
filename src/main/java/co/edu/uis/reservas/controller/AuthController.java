package co.edu.uis.reservas.controller;

import co.edu.uis.reservas.dto.RegistroForm;
import co.edu.uis.reservas.exception.ReglaNegocioException;
import co.edu.uis.reservas.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) { this.usuarioService = usuarioService; }

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/registro")
    public String registroForm(Model model) {
        model.addAttribute("registroForm", new RegistroForm());
        return "registro";
    }

    @PostMapping("/registro")
    public String registrar(@Valid @ModelAttribute("registroForm") RegistroForm form, BindingResult result) {
        if (result.hasErrors()) return "registro";
        try {
            usuarioService.registrar(form);
        } catch (ReglaNegocioException e) {
            result.rejectValue("email", "duplicado", e.getMessage());
            return "registro";
        }
        return "redirect:/login?registrado";
    }

    @GetMapping("/acceso-denegado")
    public String accesoDenegado() { return "acceso-denegado"; }
}
