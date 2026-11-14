package co.edu.uis.reservas.service;

import co.edu.uis.reservas.domain.Espacio;
import co.edu.uis.reservas.domain.EstadoReserva;
import co.edu.uis.reservas.dto.EspacioForm;
import co.edu.uis.reservas.exception.ReglaNegocioException;
import co.edu.uis.reservas.repository.EspacioRepository;
import co.edu.uis.reservas.repository.ReservaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EspacioService {

    private final EspacioRepository espacioRepository;
    private final ReservaRepository reservaRepository;

    public EspacioService(EspacioRepository espacioRepository, ReservaRepository reservaRepository) {
        this.espacioRepository = espacioRepository;
        this.reservaRepository = reservaRepository;
    }

    // REQ-44: solo se listan los espacios activos; los eliminados siguen en la base con su historial
    @Transactional(readOnly = true)
    public List<Espacio> listarActivos() {
        return espacioRepository.findByActivoTrueOrderByNombreAsc();
    }

    // REQ-42, REQ-43: busca un espacio que exista y no esté eliminado
    @Transactional(readOnly = true)
    public Espacio obtenerActivo(Long id) {
        return espacioRepository.findById(id)
                .filter(Espacio::isActivo)
                .orElseThrow(() -> new ReglaNegocioException("El espacio no existe o ya fue eliminado."));
    }

    // REQ-40, REQ-41: agrega un espacio nuevo rechazando nombres repetidos
    @Transactional
    public Espacio crear(EspacioForm form) {
        String nombre = form.getNombre().trim();
        validarNombreLibre(nombre);
        Espacio espacio = new Espacio(nombre, form.getTipo(), form.getCapacidad(), form.getUbicacion().trim());
        return guardar(espacio);
    }

    // REQ-42, REQ-41: edita nombre, tipo, capacidad y ubicación (la tarifa no se toca)
    @Transactional
    public Espacio editar(Long id, EspacioForm form) {
        Espacio espacio = obtenerActivo(id);
        String nombre = form.getNombre().trim();
        // Solo se revisa el nombre si cambió; así un espacio no choca consigo mismo
        if (!espacio.getNombre().equalsIgnoreCase(nombre)) {
            validarNombreLibre(nombre);
        }
        espacio.setNombre(nombre);
        espacio.setTipo(form.getTipo());
        espacio.setCapacidad(form.getCapacidad());
        espacio.setUbicacion(form.getUbicacion().trim());
        return guardar(espacio);
    }

    // REQ-43, REQ-44: eliminación lógica (activo = false) solo si no hay reservas futuras activas
    @Transactional
    public void eliminar(Long id) {
        // Se bloquea el espacio mientras se decide, para que nadie reserve justo en ese momento
        Espacio espacio = espacioRepository.findByIdForUpdate(id)
                .filter(Espacio::isActivo)
                .orElseThrow(() -> new ReglaNegocioException("El espacio no existe o ya fue eliminado."));

        long futuras = reservaRepository.countByEspacioIdAndEstadoInAndFinGreaterThan(
                id, EstadoReserva.ACTIVAS, LocalDateTime.now());
        if (futuras > 0) {
            throw new ReglaNegocioException("No se puede eliminar «" + espacio.getNombre() + "»: tiene "
                    + futuras + " reserva(s) futura(s) activa(s). Cancélalas o espera a que terminen.");
        }
        espacio.setActivo(false);
        espacioRepository.save(espacio);
    }

    // REQ-41: rechaza el nombre si ya lo usa otro espacio (sin importar mayúsculas)
    private void validarNombreLibre(String nombre) {
        if (espacioRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ReglaNegocioException("Ya existe un espacio con el nombre «" + nombre + "». Elige otro nombre.");
        }
    }

    // REQ-40, REQ-41, REQ-42: guarda y convierte el error de la base (nombre repetido a la vez) en mensaje claro
    private Espacio guardar(Espacio espacio) {
        try {
            return espacioRepository.saveAndFlush(espacio);
        } catch (DataIntegrityViolationException ex) {
            throw new ReglaNegocioException("Ya existe un espacio con ese nombre. Elige otro nombre.");
        }
    }
}