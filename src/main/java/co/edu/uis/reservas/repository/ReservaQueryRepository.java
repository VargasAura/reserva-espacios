package co.edu.uis.reservas.repository;

import co.edu.uis.reservas.domain.EstadoReserva;
import co.edu.uis.reservas.domain.Reserva;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Consultas propias del módulo de reservas (REQ-22). "join fetch" trae el espacio junto con la reserva. */
public interface ReservaQueryRepository extends Repository<Reserva, Long> {

    @Query("select r from Reserva r join fetch r.espacio "
            + "where r.usuario.id = :usuarioId order by r.inicio desc")
    List<Reserva> misReservas(@Param("usuarioId") Long usuarioId);

    @Query("select r from Reserva r join fetch r.espacio "
            + "where r.usuario.id = :usuarioId and r.estado = :estado order by r.inicio desc")
    List<Reserva> misReservasPorEstado(@Param("usuarioId") Long usuarioId,
                                       @Param("estado") EstadoReserva estado);
}