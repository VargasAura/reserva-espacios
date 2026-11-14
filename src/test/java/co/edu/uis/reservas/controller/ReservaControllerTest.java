package co.edu.uis.reservas.controller;

import co.edu.uis.reservas.domain.EstadoReserva;
import co.edu.uis.reservas.dto.ReservaForm;
import co.edu.uis.reservas.exception.ReglaNegocioException;
import co.edu.uis.reservas.service.ReservaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas del controlador de reservas (REQ-15, REQ-16, REQ-17, REQ-19, REQ-22). El servicio se simula con Mockito. */
@ExtendWith(MockitoExtension.class)
class ReservaControllerTest {

    private static final String EMAIL = "ana@uis.edu.co";
    private final Principal principal = () -> EMAIL;

    @Mock
    private ReservaService servicio;

    private ReservaController controlador() {
        return new ReservaController(servicio);
    }

    private BindingResult resultadoVacio(ReservaForm form) {
        return new BeanPropertyBindingResult(form, "reservaForm");
    }

    @Test
    void crearBienRedirigeALaListaConAviso() {
        ReservaForm form = new ReservaForm();
        var flash = new RedirectAttributesModelMap();

        String vista = controlador().crear(form, resultadoVacio(form), principal, new ConcurrentModel(), flash);

        assertEquals("redirect:/reservas", vista);
        assertTrue(flash.getFlashAttributes().get("exito").toString().contains("RESERVADA"));
        verify(servicio).crear(eq(EMAIL), any());
    }

    @Test
    void crearConConflictoVuelveAlFormularioConErrorGlobal() {
        ReservaForm form = new ReservaForm();
        BindingResult result = resultadoVacio(form);
        when(servicio.crear(eq(EMAIL), any())).thenThrow(new ReglaNegocioException("El horario no está disponible"));

        String vista = controlador().crear(form, result, principal, new ConcurrentModel(), new RedirectAttributesModelMap());

        assertEquals("reservas/formulario", vista);
        assertEquals("El horario no está disponible", result.getGlobalError().getDefaultMessage());
    }

    @Test
    void crearConDatosIncompletosNoLlamaAlServicio() {
        ReservaForm form = new ReservaForm();
        BindingResult result = resultadoVacio(form);
        result.rejectValue("fecha", "obligatorio", "La fecha es obligatoria");

        String vista = controlador().crear(form, result, principal, new ConcurrentModel(), new RedirectAttributesModelMap());

        assertEquals("reservas/formulario", vista);
        verify(servicio, never()).crear(any(), any());
    }

    @Test
    void confirmarConErrorMuestraElMotivo() {
        when(servicio.confirmar(5L, EMAIL)).thenThrow(new ReglaNegocioException("El horario no está disponible"));
        var flash = new RedirectAttributesModelMap();

        String vista = controlador().confirmar(5L, principal, flash);

        assertEquals("redirect:/reservas", vista);
        assertEquals("El horario no está disponible", flash.getFlashAttributes().get("error"));
    }

    @Test
    void cancelarBienAvisaQueElHorarioQuedoLibre() {
        var flash = new RedirectAttributesModelMap();

        controlador().cancelar(7L, principal, flash);

        verify(servicio).cancelar(7L, EMAIL);
        assertTrue(flash.getFlashAttributes().get("exito").toString().contains("disponible"));
    }

    @Test
    void listaPasaElFiltroDeEstadoAlServicio() {
        when(servicio.misReservas(EMAIL, EstadoReserva.CONFIRMADA)).thenReturn(List.of());
        var model = new ConcurrentModel();

        String vista = controlador().lista(EstadoReserva.CONFIRMADA, principal, model);

        assertEquals("reservas/lista", vista);
        assertEquals(EstadoReserva.CONFIRMADA, model.getAttribute("estado"));
    }
}