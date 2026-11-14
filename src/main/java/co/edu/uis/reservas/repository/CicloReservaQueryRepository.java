package co.edu.uis.reservas.repository;

import co.edu.uis.reservas.domain.EstadoReserva;
import co.edu.uis.reservas.domain.Reserva;
import org.springframework.data.repository.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/** Consultas propias del ciclo de vida de las reservas (REQ-18, 23, 24, 25, 34). Spring arma cada consulta por el nombre. */
public interface CicloReservaQueryRepository extends Repository<Reserva, Long> {

    /** REQ-18: reservas en un estado creadas antes de cierta hora (plazo de confirmación vencido). */
    List<Reserva> findByEstadoAndCreadaEnBefore(EstadoReserva estado, LocalDateTime limite);

    /** REQ-25: reservas en un estado cuyo inicio ya pasó más de cierto tiempo. */
    List<Reserva> findByEstadoAndInicioBefore(EstadoReserva estado, LocalDateTime limite);

    /** REQ-24: reservas en un estado cuya hora de fin ya llegó. */
    List<Reserva> findByEstadoAndFinLessThanEqual(EstadoReserva estado, LocalDateTime ahora);

    /** REQ-34: reservas en un estado que empiezan entre dos momentos. */
    List<Reserva> findByEstadoAndInicioBetween(EstadoReserva estado, LocalDateTime desde, LocalDateTime hasta);

    /** REQ-23, REQ-24: reservas en varios estados, las más próximas primero (pantalla del administrador). */
    List<Reserva> findByEstadoInOrderByInicioAsc(Collection<EstadoReserva> estados);
}