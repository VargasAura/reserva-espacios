package co.edu.uis.reservas.controller;

import co.edu.uis.reservas.domain.TipoEspacio;
import co.edu.uis.reservas.service.CalendarioService;
import co.edu.uis.reservas.service.CalendarioService.Franja;
import co.edu.uis.reservas.service.CalendarioService.Vista;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

/** Pruebas del controlador del calendario (REQ-10, REQ-11): valores por defecto y valores inválidos. El servicio se simula con Mockito. */
@ExtendWith(MockitoExtension.class)
class CalendarioControllerTest {

    @Mock
    private CalendarioService servicio;

    private CalendarioController controlador() {
        return new CalendarioController(servicio);
    }

    @Test
    void sinParametrosUsaLosValoresPorDefecto() {
        controlador().datos(null, null, null, null);

        verify(servicio).consultar(Vista.DIA, LocalDate.now(), null, Franja.DIA_COMPLETO);
    }

    @Test
    void valoresInvalidosNoDanErrorYUsanLosPorDefecto() {
        controlador().datos("YEARLY", "no-es-fecha", "PISCINA", "MEDIANOCHE");

        verify(servicio).consultar(Vista.DIA, LocalDate.now(), null, Franja.DIA_COMPLETO);
    }

    @Test
    void valoresValidosSeRespetanAunqueVenganEnMinuscula() {
        controlador().datos("semana", "2026-11-20", "auditorio", "tarde");

        verify(servicio).consultar(Vista.SEMANA, LocalDate.of(2026, 11, 20), TipoEspacio.AUDITORIO, Franja.TARDE);
    }

    @Test
    void tipoVacioSignificaTodosLosTipos() {
        controlador().datos("MES", "2026-12-01", "", "NOCHE");

        verify(servicio).consultar(Vista.MES, LocalDate.of(2026, 12, 1), null, Franja.NOCHE);
    }

    @Test
    void paginaEntregaLosFiltrosYLaVista() {
        var model = new ConcurrentModel();

        String vista = controlador().pagina(model);

        assertEquals("calendario/calendario", vista);
        assertArrayEquals(Vista.values(), (Vista[]) model.getAttribute("vistas"));
        assertArrayEquals(TipoEspacio.values(), (TipoEspacio[]) model.getAttribute("tipos"));
        assertArrayEquals(Franja.values(), (Franja[]) model.getAttribute("franjas"));
    }
}