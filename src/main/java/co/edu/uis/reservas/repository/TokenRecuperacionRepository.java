package co.edu.uis.reservas.repository;

import co.edu.uis.reservas.domain.TokenRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {
    Optional<TokenRecuperacion> findByTokenHash(String tokenHash);
    Optional<TokenRecuperacion> findTopByUsuarioIdOrderByCreadoEnDesc(Long usuarioId);
    void deleteByUsuarioId(Long usuarioId);
}
