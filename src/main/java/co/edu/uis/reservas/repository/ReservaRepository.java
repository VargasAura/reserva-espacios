package co.edu.uis.reservas.repository;

import co.edu.uis.reservas.domain.EstadoReserva;
import co.edu.uis.reservas.domain.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    /**
     * REQ-28: dos horarios se superponen cuando el inicio de cada uno es anterior al fin del otro.
     * Reservas consecutivas (fin = inicio) NO son conflicto.
     * Para editar una reserva, pasar su id en 'excluirId' para que no choque consigo misma (si es nueva, pasar -1).
     */
    @Query("""
           select count(r) > 0 from Reserva r
           where r.espacio.id = :espacioId
             and r.estado in :estados
             and r.id <> :excluirId
             and r.inicio < :fin
             and r.fin > :inicio
           """)
    boolean existeSolape(@Param("espacioId") Long espacioId,
                         @Param("inicio") LocalDateTime inicio,
                         @Param("fin") LocalDateTime fin,
                         @Param("estados") Collection<EstadoReserva> estados,
                         @Param("excluirId") Long excluirId);

    List<Reserva> findByUsuarioIdOrderByInicioDesc(Long usuarioId);

    List<Reserva> findByEspacioIdAndEstadoInAndInicioLessThanAndFinGreaterThan(
            Long espacioId, Collection<EstadoReserva> estados, LocalDateTime finDia, LocalDateTime inicioDia);

    long countByEspacioIdAndEstadoInAndFinGreaterThan(Long espacioId, Collection<EstadoReserva> estados, LocalDateTime desde);
}
