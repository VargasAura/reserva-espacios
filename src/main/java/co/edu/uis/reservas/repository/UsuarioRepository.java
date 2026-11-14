package co.edu.uis.reservas.repository;

import co.edu.uis.reservas.domain.Rol;
import co.edu.uis.reservas.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    long countByRolAndActivoTrue(Rol rol); // REQ-50: no dejar el sistema sin administrador
}
