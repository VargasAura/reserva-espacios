package co.edu.uis.reservas.config;

import co.edu.uis.reservas.domain.Espacio;
import co.edu.uis.reservas.domain.Rol;
import co.edu.uis.reservas.domain.TipoEspacio;
import co.edu.uis.reservas.domain.Usuario;
import co.edu.uis.reservas.repository.EspacioRepository;
import co.edu.uis.reservas.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Crea un administrador y algunos espacios de ejemplo la primera vez que se ejecuta el sistema. */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarios;
    private final EspacioRepository espacios;
    private final PasswordEncoder encoder;

    public DataInitializer(UsuarioRepository usuarios, EspacioRepository espacios, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.espacios = espacios;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (usuarios.count() == 0) {
            Usuario admin = new Usuario();
            admin.setNombre("Administrador");
            admin.setEmail("admin@reservas.co");
            admin.setPassword(encoder.encode("Admin1234"));
            admin.setTelefono("3000000000");
            admin.setRol(Rol.ADMIN);
            usuarios.save(admin);
        }
        if (espacios.count() == 0) {
            espacios.save(new Espacio("Sala Guadua", TipoEspacio.SALA_REUNIONES, 12, "Edificio A, piso 2"));
            espacios.save(new Espacio("Auditorio Roble", TipoEspacio.AUDITORIO, 150, "Edificio B, piso 1"));
            espacios.save(new Espacio("Cancha Ceiba", TipoEspacio.CANCHA_DEPORTIVA, 20, "Zona deportiva"));
            Espacio kiosco = new Espacio("Kiosco del Jardín", TipoEspacio.AREA_RECREATIVA, 30, "Jardín central");
            kiosco.setTarifaHora(new BigDecimal("15000"));
            espacios.save(kiosco);
        }
    }
}
