package co.edu.uis.reservas.controller;

import co.edu.uis.reservas.exception.ReglaNegocioException;
import co.edu.uis.reservas.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class HomeController {

    private final UsuarioService usuarioService;

    public HomeController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/")
    public String raiz() { return "redirect:/inicio"; }

    // El saludo usa el nombre de la persona; la sesión solo trae el correo, así que se busca el usuario.
    @GetMapping("/inicio")
    public String inicio(Principal principal, Model model) {
        String nombre;
        try {
            nombre = usuarioService.buscarPorEmail(principal.getName()).getNombre();
        } catch (ReglaNegocioException e) {
            nombre = principal.getName(); // no debería pasar; así la pantalla nunca falla
        }
        model.addAttribute("nombre", nombre);
        return "inicio";
    }
}
