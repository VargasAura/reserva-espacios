package co.edu.uis.reservas.controller;

import co.edu.uis.reservas.domain.EstadoReserva;
import co.edu.uis.reservas.dto.ReservaForm;
import co.edu.uis.reservas.exception.ReglaNegocioException;
import co.edu.uis.reservas.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/** Pantallas de "Mis reservas" (REQ-15 a REQ-22). El usuario sale de la sesión: principal.getName() es su correo. */
@Controller
@RequestMapping("/reservas")
public class ReservaController {

    private static final String VISTA_LISTA = "reservas/lista";
    private static final String VISTA_FORM = "reservas/formulario";
    private static final String REDIRIGIR_LISTA = "redirect:/reservas";

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    // REQ-22: lista de mis reservas con filtro opcional por estado (?estado=CONFIRMADA)
    @GetMapping
    public String lista(@RequestParam(required = false) EstadoReserva estado, Principal principal, Model model) {
        model.addAttribute("reservas", reservaService.misReservas(principal.getName(), estado));
        model.addAttribute("estado", estado);
        model.addAttribute("estados", EstadoReserva.values());
        return VISTA_LISTA;
    }

    // REQ-15: formulario vacío; el calendario puede precargar espacioId, fecha, horaInicio y horaFin por la URL
    @GetMapping("/nueva")
    public String nueva(@ModelAttribute("reservaForm") ReservaForm form, BindingResult ignorado, Model model) {
        form.setId(null);
        return vistaFormulario(model);
    }

    // REQ-15, REQ-16: crea la reserva en estado RESERVADA
    @PostMapping
    public String crear(@Valid @ModelAttribute("reservaForm") ReservaForm form, BindingResult result,
                        Principal principal, Model model, RedirectAttributes flash) {
        form.setId(null);
        if (result.hasErrors()) return vistaFormulario(model);
        try {
            reservaService.crear(principal.getName(), form);
            flash.addFlashAttribute("exito",
                    "Reserva creada en estado RESERVADA: confírmala antes de que venza el plazo.");
            return REDIRIGIR_LISTA;
        } catch (ReglaNegocioException e) {
            result.reject("regla", e.getMessage());
            return vistaFormulario(model);
        }
    }

    // REQ-21: formulario de edición con los datos actuales de la reserva
    @GetMapping("/{id}/editar")
    public String editarForm(@PathVariable Long id, Principal principal, Model model, RedirectAttributes flash) {
        try {
            model.addAttribute("reservaForm", reservaService.formularioParaEditar(id, principal.getName()));
            return vistaFormulario(model);
        } catch (ReglaNegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return REDIRIGIR_LISTA;
        }
    }

    // REQ-21, REQ-16: guarda el nuevo horario; si hay conflicto, la reserva original no se toca
    @PostMapping("/{id}")
    public String editar(@PathVariable Long id, @Valid @ModelAttribute("reservaForm") ReservaForm form,
                         BindingResult result, Principal principal, Model model, RedirectAttributes flash) {
        form.setId(id);
        if (result.hasErrors()) return vistaFormulario(model);
        try {
            reservaService.editar(id, principal.getName(), form);
            flash.addFlashAttribute("exito", "Reserva actualizada correctamente.");
            return REDIRIGIR_LISTA;
        } catch (ReglaNegocioException e) {
            result.reject("regla", e.getMessage());
            return vistaFormulario(model);
        }
    }

    // REQ-17: RESERVADA -> CONFIRMADA
    @PostMapping("/{id}/confirmar")
    public String confirmar(@PathVariable Long id, Principal principal, RedirectAttributes flash) {
        try {
            reservaService.confirmar(id, principal.getName());
            flash.addFlashAttribute("exito", "Reserva confirmada correctamente.");
        } catch (ReglaNegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return REDIRIGIR_LISTA;
    }

    // REQ-19, REQ-20: cancelar (el navegador pide confirmación antes de enviar)
    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Long id, Principal principal, RedirectAttributes flash) {
        try {
            reservaService.cancelar(id, principal.getName());
            flash.addFlashAttribute("exito", "Reserva cancelada. El horario quedó disponible.");
        } catch (ReglaNegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return REDIRIGIR_LISTA;
    }

    // REQ-15, REQ-21: agrega la lista de espacios al formulario y devuelve la vista
    private String vistaFormulario(Model model) {
        model.addAttribute("espacios", reservaService.espaciosDisponibles());
        return VISTA_FORM;
    }
}