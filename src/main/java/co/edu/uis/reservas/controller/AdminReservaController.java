package co.edu.uis.reservas.controller;

import co.edu.uis.reservas.exception.ReglaNegocioException;
import co.edu.uis.reservas.service.CicloReservaService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Panel del administrador para registrar el inicio y el fin del uso de las reservas. */
@Controller
@RequestMapping("/admin/reservas")
public class AdminReservaController {

    private final CicloReservaService ciclo;

    public AdminReservaController(CicloReservaService ciclo) { this.ciclo = ciclo; }

    // REQ-23, REQ-24: muestra las reservas CONFIRMADAS y EN_USO
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("filas", ciclo.filasParaAdmin());
        return "admin/reservas-uso";
    }

    // REQ-23, REQ-27: el admin registra el inicio del uso (responsable = su correo)
    @PostMapping("/{id}/iniciar")
    public String iniciar(@PathVariable Long id, Authentication auth, RedirectAttributes flash) {
        try {
            ciclo.iniciarUso(id, auth.getName());
            flash.addFlashAttribute("exito", "Se registró el inicio del uso de la reserva #" + id + ".");
        } catch (ReglaNegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/reservas";
    }

    // REQ-24, REQ-26, REQ-27: el admin registra la finalización (responsable = su correo)
    @PostMapping("/{id}/finalizar")
    public String finalizar(@PathVariable Long id, Authentication auth, RedirectAttributes flash) {
        try {
            ciclo.finalizar(id, auth.getName());
            flash.addFlashAttribute("exito", "Reserva #" + id + " finalizada. El espacio quedó disponible.");
        } catch (ReglaNegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/reservas";
    }
}