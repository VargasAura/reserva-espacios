package co.edu.uis.reservas.repository;

import co.edu.uis.reservas.domain.Espacio;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EspacioRepository extends JpaRepository<Espacio, Long> {

    List<Espacio> findByActivoTrueOrderByNombreAsc();
    boolean existsByNombreIgnoreCase(String nombre);

    /**
     * Bloquea la fila del espacio hasta terminar la transacción.
     * Así, dos reservas simultáneas del mismo espacio se atienden una tras otra (REQ-31).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Espacio e where e.id = :id")
    Optional<Espacio> findByIdForUpdate(@Param("id") Long id);
}
