package co.edu.uis.reservas.controller;

import co.edu.uis.reservas.domain.Usuario;
import co.edu.uis.reservas.exception.ReglaNegocioException;
import co.edu.uis.reservas.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HomeControllerTest {

    private final Principal principal = () -> "ana@uis.edu.co";

    @Test
    void elSaludoUsaElNombreYNoElCorreo() {
        UsuarioService usuarios = mock(UsuarioService.class);
        Usuario ana = new Usuario();
        ana.setNombre("Ana Pérez");
        when(usuarios.buscarPorEmail("ana@uis.edu.co")).thenReturn(ana);
        var model = new ConcurrentModel();

        String vista = new HomeController(usuarios).inicio(principal, model);

        assertEquals("inicio", vista);
        assertEquals("Ana Pérez", model.getAttribute("nombre"));
    }

    @Test
    void siNoSeEncuentraElUsuarioNoFalla() {
        UsuarioService usuarios = mock(UsuarioService.class);
        when(usuarios.buscarPorEmail("ana@uis.edu.co")).thenThrow(new ReglaNegocioException("no existe"));
        var model = new ConcurrentModel();

        new HomeController(usuarios).inicio(principal, model);

        assertEquals("ana@uis.edu.co", model.getAttribute("nombre"));
    }
}
