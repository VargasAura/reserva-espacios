package co.edu.uis.reservas.repository;

import co.edu.uis.reservas.domain.Espacio;
import co.edu.uis.reservas.domain.Reserva;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/** Consultas de solo lectura para los reportes de uso (REQ-51 a REQ-53). */
public interface ReporteQueryRepository extends Repository<Reserva, Long> {

    // REQ-51, REQ-44: reservas del periodo (incluye espacios eliminados lógicamente; no se filtra por 'activo').
    // "hasta" es exclusivo: se pasa el día siguiente a las 00:00.
    @Query("select r from Reserva r join fetch r.espacio "
            + "where r.inicio >= :desde and r.inicio < :hasta")
    List<Reserva> reservasEnPeriodo(@Param("desde") LocalDateTime desde,
                                    @Param("hasta") LocalDateTime hasta);

    // REQ-51: todos los espacios (activos y eliminados) para el filtro y para listar espacios sin reservas.
    @Query("select e from Espacio e order by e.nombre")
    List<Espacio> todosLosEspacios();
}