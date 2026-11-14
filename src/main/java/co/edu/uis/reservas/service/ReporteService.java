package co.edu.uis.reservas.service;

import co.edu.uis.reservas.domain.Espacio;
import co.edu.uis.reservas.domain.EstadoReserva;
import co.edu.uis.reservas.domain.Reserva;
import co.edu.uis.reservas.domain.TipoEspacio;
import co.edu.uis.reservas.exception.ReglaNegocioException;
import co.edu.uis.reservas.repository.ReporteQueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Reportes de uso de espacios (REQ-51 a REQ-55). */
@Service
public class ReporteService {

    /**
     * Ventana diaria en la que se puede reservar: de 06:00 a 22:00 = 16 horas por día.
     * El % de ocupación = horas reservadas / (días del periodo × 16 h) × 100.
     */
    public static final LocalTime HORA_APERTURA = LocalTime.of(6, 0);
    public static final LocalTime HORA_CIERRE = LocalTime.of(22, 0);
    public static final long HORAS_DISPONIBLES_POR_DIA = Duration.between(HORA_APERTURA, HORA_CIERRE).toHours();

    /** Estados que cuentan como horas realmente reservadas. */
    private static final Set<EstadoReserva> ESTADOS_QUE_OCUPAN =
            EnumSet.of(EstadoReserva.FINALIZADA, EstadoReserva.CONFIRMADA, EstadoReserva.EN_USO);

    /** Una fila de la tabla por espacio (REQ-52). */
    public record FilaEspacio(String nombre, String tipo, boolean eliminado,
                              long totalReservas, double horasReservadas, double ocupacion) {
        /** Ancho de la barra (máximo 100%). */
        public double barra() { return Math.min(100.0, ocupacion); }
    }

    /** Una barra del gráfico por estado (REQ-53). */
    public record BarraEstado(String etiqueta, String clase, long cantidad, double porcentaje) {}

    /** Resultado completo del reporte. */
    public record Reporte(LocalDate desde, LocalDate hasta, long totalReservas, double horasReservadas,
                          double ocupacionGlobal, List<BarraEstado> estados, List<FilaEspacio> filas) {
        public boolean hayDatos() { return totalReservas > 0; }
    }

    private final ReporteQueryRepository repo;

    public ReporteService(ReporteQueryRepository repo) {
        this.repo = repo;
    }

    // REQ-51: espacios (activos y eliminados) para el desplegable de filtros.
    @Transactional(readOnly = true)
    public List<Espacio> listarEspaciosParaFiltro() {
        return repo.todosLosEspacios();
    }

    // REQ-51, REQ-52, REQ-53, REQ-44, REQ-55
    @Transactional(readOnly = true)
    public Reporte generar(LocalDate desde, LocalDate hasta, Long espacioId, TipoEspacio tipo) {
        if (hasta.isBefore(desde)) {
            throw new ReglaNegocioException("La fecha final no puede ser anterior a la fecha inicial.");
        }
        long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;

        List<Reserva> reservas = repo.reservasEnPeriodo(desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay())
                .stream().filter(r -> coincide(r.getEspacio(), espacioId, tipo)).toList();

        Map<Espacio, List<Reserva>> porEspacio = new HashMap<>();
        for (Reserva r : reservas) {
            porEspacio.computeIfAbsent(r.getEspacio(), k -> new ArrayList<>()).add(r);
        }

        // Espacios activos + eliminados que sí tienen reservas en el periodo (REQ-44).
        List<FilaEspacio> filas = repo.todosLosEspacios().stream()
                .filter(e -> coincide(e, espacioId, tipo))
                .filter(e -> e.isActivo() || porEspacio.containsKey(e))
                .map(e -> crearFila(e, porEspacio.getOrDefault(e, List.of()), dias))
                .sorted(Comparator.comparingDouble(FilaEspacio::ocupacion).reversed()
                        .thenComparing(FilaEspacio::nombre))
                .toList();

        double horasTotales = horasQueOcupan(reservas);
        double disponibles = filas.size() * dias * (double) HORAS_DISPONIBLES_POR_DIA;
        double global = disponibles == 0 ? 0 : horasTotales * 100.0 / disponibles;

        return new Reporte(desde, hasta, reservas.size(), redondear(horasTotales), redondear(global),
                contarPorEstado(reservas), filas);
    }

    // REQ-52: total de reservas, horas reservadas y % de ocupación de un espacio.
    private FilaEspacio crearFila(Espacio e, List<Reserva> lista, long dias) {
        double horas = horasQueOcupan(lista);
        double ocupacion = horas * 100.0 / (dias * (double) HORAS_DISPONIBLES_POR_DIA);
        return new FilaEspacio(e.getNombre(), e.getTipo().getEtiqueta(), !e.isActivo(),
                lista.size(), redondear(horas), redondear(ocupacion));
    }

    // REQ-53: cantidad de reservas Finalizadas, Canceladas y NoShow.
    private List<BarraEstado> contarPorEstado(List<Reserva> reservas) {
        long total = reservas.size();
        return List.of(
                barra("Finalizadas", EstadoReserva.FINALIZADA, reservas, total),
                barra("Canceladas", EstadoReserva.CANCELADA, reservas, total),
                barra("No asistió", EstadoReserva.NO_SHOW, reservas, total));
    }

    private BarraEstado barra(String etiqueta, EstadoReserva estado, List<Reserva> reservas, long total) {
        long cantidad = reservas.stream().filter(r -> r.getEstado() == estado).count();
        double pct = total == 0 ? 0 : cantidad * 100.0 / total;
        return new BarraEstado(etiqueta, estado.name(), cantidad, redondear(pct));
    }

    // REQ-52: suma de horas de las reservas FINALIZADA, CONFIRMADA o EN_USO.
    private double horasQueOcupan(List<Reserva> reservas) {
        long minutos = reservas.stream()
                .filter(r -> ESTADOS_QUE_OCUPAN.contains(r.getEstado()))
                .mapToLong(r -> Duration.between(r.getInicio(), r.getFin()).toMinutes())
                .sum();
        return minutos / 60.0;
    }

    private boolean coincide(Espacio e, Long espacioId, TipoEspacio tipo) {
        return (espacioId == null || espacioId.equals(e.getId()))
                && (tipo == null || e.getTipo() == tipo);
    }

    private double redondear(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }
}