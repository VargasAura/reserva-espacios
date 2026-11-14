package co.edu.uis.reservas.controller;

import co.edu.uis.reservas.domain.Rol;
import co.edu.uis.reservas.exception.ReglaNegocioException;
import co.edu.uis.reservas.service.AdminUsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Administración de usuarios (REQ-46 a REQ-50). /admin/** ya es solo ADMIN en SecurityConfig. */
@Controller
@RequestMapping("/admin/usuarios")
public class AdminUsuarioController {

    private static final String REDIRIGIR_LISTA = "redirect:/admin/usuarios";

    private final AdminUsuarioService adminUsuarioService;

    public AdminUsuarioController(AdminUsuarioService adminUsuarioService) {
        this.adminUsuarioService = adminUsuarioService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String q,
                         @RequestParam(required = false) Rol rol,
                         @RequestParam(required = false) Boolean activo,
                         @RequestParam(required = false) Long verReservas,
                         Model model) {
        model.addAttribute("usuarios", adminUsuarioService.listar(q, rol, activo));
        model.addAttribute("roles", Rol.values());
        // Valores para que los filtros se queden escritos después de buscar
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("filtroRol", rol == null ? "" : rol.name());
        model.addAttribute("filtroActivo", activo == null ? "" : activo.toString());
        if (verReservas != null) {
            try {
                model.addAttribute("seleccionado", adminUsuarioService.reservasDe(verReservas));
            } catch (ReglaNegocioException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return "admin/usuarios";
    }

    @PostMapping("/{id}/rol")
    public String cambiarRol(@PathVariable Long id, @RequestParam Rol rol, RedirectAttributes flash) {
        try {
            adminUsuarioService.cambiarRol(id, rol);
            flash.addFlashAttribute("exito", "Rol actualizado correctamente.");
        } catch (ReglaNegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return REDIRIGIR_LISTA;
    }

    @PostMapping("/{id}/activo")
    public String cambiarActivo(@PathVariable Long id, @RequestParam boolean activo, RedirectAttributes flash) {
        try {
            adminUsuarioService.cambiarActivo(id, activo);
            flash.addFlashAttribute("exito", activo ? "Cuenta activada." : "Cuenta desactivada. Su historial se conserva.");
        } catch (ReglaNegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return REDIRIGIR_LISTA;
    }

    @PostMapping("/reservas/{reservaId}/cancelar")
    public String cancelarReserva(@PathVariable Long reservaId, RedirectAttributes flash) {
        try {
            Long usuarioId = adminUsuarioService.cancelarReserva(reservaId);
            flash.addFlashAttribute("exito", "Reserva cancelada y usuario notificado.");
            return REDIRIGIR_LISTA + "?verReservas=" + usuarioId;
        } catch (ReglaNegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return REDIRIGIR_LISTA;
        }
    }
}