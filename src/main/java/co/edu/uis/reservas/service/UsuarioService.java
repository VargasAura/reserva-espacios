package co.edu.uis.reservas.service;

import co.edu.uis.reservas.domain.Rol;
import co.edu.uis.reservas.domain.Usuario;
import co.edu.uis.reservas.dto.RegistroForm;
import co.edu.uis.reservas.exception.ReglaNegocioException;
import co.edu.uis.reservas.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    /** REQ-1, REQ-2, REQ-3: autorregistro siempre como usuario regular. */
    @Transactional
    public Usuario registrar(RegistroForm f) {
        if (usuarios.existsByEmailIgnoreCase(f.getEmail())) {
            throw new ReglaNegocioException("Este correo ya está registrado");
        }
        Usuario u = new Usuario();
        u.setNombre(f.getNombre().trim());
        u.setEmail(f.getEmail().trim().toLowerCase());
        u.setPassword(encoder.encode(f.getPassword()));
        u.setTelefono(f.getTelefono().trim());
        u.setRol(Rol.USUARIO);
        return usuarios.save(u);
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorEmail(String email) {
        return usuarios.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ReglaNegocioException("Usuario no encontrado"));
    }
}
