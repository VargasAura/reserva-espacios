package co.edu.uis.reservas.service;

import co.edu.uis.reservas.config.ReglasNegocio;
import co.edu.uis.reservas.domain.Espacio;
import co.edu.uis.reservas.domain.EstadoReserva;
import co.edu.uis.reservas.domain.Reserva;
import co.edu.uis.reservas.domain.TipoEspacio;
import co.edu.uis.reservas.repository.EspacioRepository;
import co.edu.uis.reservas.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CalendarioService {

    /** REQ-14: cada cuántos segundos se refresca la pantalla (debe ser menor a 5). */
    public static final int REFRESCO_SEGUNDOS = 3;

    /** Cada casilla del calendario dura lo mismo que la reserva más corta permitida. */
    private static final int MINUTOS_FRANJA = (int) ReglasNegocio.DURACION_MINIMA.toMinutes();
    private static final int MINUTOS_POR_HORA = 60;
    private static final int DIAS_SEMANA = 7;

    /** Letras con que se describe cada casilla: libre, reservada o bloqueada (horario ya pasado). */
    private static final char LIBRE = 'L';
    private static final char RESERVADO = 'R';
    private static final char BLOQUEADO = 'B';

    public enum Vista { DIA, SEMANA, MES }

    /** Franja horaria para filtrar (REQ-11). */
    public enum Franja {
        DIA_COMPLETO("Todo el día", 0, 24),
        MANANA("Mañana (06:00–12:00)", 6, 12),
        TARDE("Tarde (12:00–18:00)", 12, 18),
        NOCHE("Noche (18:00–24:00)", 18, 24);

        private final String etiqueta;
        private final int horaDesde;
        private final int horaHasta;

        Franja(String etiqueta, int horaDesde, int horaHasta) {
            this.etiqueta = etiqueta;
            this.horaDesde = horaDesde;
            this.horaHasta = horaHasta;
        }

        public String getEtiqueta() { return etiqueta; }
        int minutosDesde() { return horaDesde * MINUTOS_POR_HORA; }
        int minutosHasta() { return horaHasta * MINUTOS_POR_HORA; }
    }

    /** Datos que usa el JavaScript para dibujar (inicio de la franja en minutos, duración de casilla, refresco). */
    public record Config(int inicioMinutos, int franjaMinutos, int refrescoSegundos) {}

    /** Un espacio con su detalle (REQ-12) y, por día, una cadena con una letra por casilla (L, R o B). */
    public record EspacioDisponible(Long id, String nombre, String tipo, String tipoEtiqueta,
                                    int capacidad, String ubicacion, Map<String, String> dias) {}

    public record Disponibilidad(Config config, List<String> dias, List<EspacioDisponible> espacios) {}

    private final EspacioRepository espacioRepository;
    private final ReservaRepository reservaRepository;

    public CalendarioService(EspacioRepository espacioRepository, ReservaRepository reservaRepository) {
        this.espacioRepository = espacioRepository;
        this.reservaRepository = reservaRepository;
    }

    // REQ-10, REQ-11, REQ-12, REQ-14: disponibilidad diaria, semanal o mensual filtrada por tipo y franja
    @Transactional(readOnly = true)
    public Disponibilidad consultar(Vista vista, LocalDate fecha, TipoEspacio tipo, Franja franja) {
        LocalDate desde = switch (vista) {
            case DIA -> fecha;
            case SEMANA -> fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MES -> fecha.withDayOfMonth(1);
        };
        LocalDate hasta = switch (vista) {
            case DIA -> desde.plusDays(1);
            case SEMANA -> desde.plusDays(DIAS_SEMANA);
            case MES -> desde.plusMonths(1);
        };
        List<String> dias = desde.datesUntil(hasta).map(LocalDate::toString).toList();
        LocalDateTime ahora = LocalDateTime.now();

        List<EspacioDisponible> lista = new ArrayList<>();
        for (Espacio e : espacioRepository.findByActivoTrueOrderByNombreAsc()) {
            if (tipo != null && e.getTipo() != tipo) continue;
            // Una sola consulta por espacio para todo el periodo (solo reservas activas, REQ-28)
            List<Reserva> reservas = reservaRepository.findByEspacioIdAndEstadoInAndInicioLessThanAndFinGreaterThan(
                    e.getId(), EstadoReserva.ACTIVAS, hasta.atStartOfDay(), desde.atStartOfDay());
            Map<String, String> porDia = new LinkedHashMap<>();
            for (LocalDate dia = desde; dia.isBefore(hasta); dia = dia.plusDays(1)) {
                porDia.put(dia.toString(), estadoDelDia(dia, reservas, franja, ahora));
            }
            lista.add(new EspacioDisponible(e.getId(), e.getNombre(), e.getTipo().name(),
                    e.getTipo().getEtiqueta(), e.getCapacidad(), e.getUbicacion(), porDia));
        }
        return new Disponibilidad(new Config(franja.minutosDesde(), MINUTOS_FRANJA, REFRESCO_SEGUNDOS), dias, lista);
    }

    // REQ-10, REQ-11: una letra por casilla del día dentro de la franja elegida
    private String estadoDelDia(LocalDate dia, List<Reserva> reservas, Franja franja, LocalDateTime ahora) {
        StringBuilder sb = new StringBuilder();
        for (int minuto = franja.minutosDesde(); minuto < franja.minutosHasta(); minuto += MINUTOS_FRANJA) {
            LocalDateTime inicio = dia.atStartOfDay().plusMinutes(minuto);
            LocalDateTime fin = inicio.plusMinutes(MINUTOS_FRANJA);
            sb.append(estadoDeCasilla(inicio, fin, reservas, ahora));
        }
        return sb.toString();
    }

    // REQ-10, REQ-28: pasada = bloqueada; cruzada con una reserva activa = reservada; si no, libre
    private char estadoDeCasilla(LocalDateTime inicio, LocalDateTime fin, List<Reserva> reservas, LocalDateTime ahora) {
        if (inicio.isBefore(ahora)) return BLOQUEADO;
        for (Reserva r : reservas) {
            if (r.getInicio().isBefore(fin) && r.getFin().isAfter(inicio)) return RESERVADO;
        }
        return LIBRE;
    }
}