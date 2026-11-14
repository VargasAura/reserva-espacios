package co.edu.uis.reservas.controller;

import co.edu.uis.reservas.domain.TipoEspacio;
import co.edu.uis.reservas.exception.ReglaNegocioException;
import co.edu.uis.reservas.service.ReporteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.YearMonth;

/** Pantalla de reportes. REQ-56: /admin/** ya es solo ADMIN en SecurityConfig. */
@Controller
@RequestMapping("/admin/reportes")
public class AdminReporteController {

    private final ReporteService reporteService;

    public AdminReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    // REQ-51, REQ-52, REQ-53, REQ-54, REQ-55, REQ-56
    @GetMapping
    public String ver(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                      @RequestParam(required = false) Long espacioId,
                      @RequestParam(required = false) TipoEspacio tipo,
                      Model model) {
        // Por defecto: el mes actual.
        YearMonth mes = YearMonth.now();
        if (desde == null) desde = mes.atDay(1);
        if (hasta == null) hasta = mes.atEndOfMonth();

        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        model.addAttribute("espacioId", espacioId);
        model.addAttribute("tipo", tipo);
        model.addAttribute("tipos", TipoEspacio.values());
        model.addAttribute("espacios", reporteService.listarEspaciosParaFiltro());
        try {
            model.addAttribute("reporte", reporteService.generar(desde, hasta, espacioId, tipo));
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "admin/reportes";
    }
}