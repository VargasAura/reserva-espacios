package co.edu.uis.reservas.security;

import co.edu.uis.reservas.domain.Usuario;
import co.edu.uis.reservas.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Le dice a Spring Security cómo buscar un usuario por correo (REQ-4). Cuenta inactiva = no puede entrar (REQ-48). */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarios;

    public UsuarioDetailsService(UsuarioRepository usuarios) { this.usuarios = usuarios; }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario u = usuarios.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
        return User.builder()
                .username(u.getEmail())
                .password(u.getPassword())
                .roles(u.getRol().name())
                .disabled(!u.isActivo())
                .build();
    }
}
