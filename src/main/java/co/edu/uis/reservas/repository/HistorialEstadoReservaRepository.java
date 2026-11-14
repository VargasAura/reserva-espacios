package co.edu.uis.reservas.repository;

import co.edu.uis.reservas.domain.HistorialEstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Guarda y consulta el historial de estados (REQ-27). */
public interface HistorialEstadoReservaRepository extends JpaRepository<HistorialEstadoReserva, Long> {
    List<HistorialEstadoReserva> findByReservaIdOrderByFechaHoraAsc(Long reservaId);
}