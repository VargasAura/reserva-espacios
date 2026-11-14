package co.edu.uis.reservas.scheduler;

import co.edu.uis.reservas.service.CicloReservaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Tarea automática que corre cada minuto y hace avanzar el ciclo de vida de las reservas. */
@Component
public class ReservasScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservasScheduler.class);

    private final CicloReservaService ciclo;

    public ReservasScheduler(CicloReservaService ciclo) { this.ciclo = ciclo; }

    // REQ-18, REQ-24, REQ-25, REQ-26: libera vencidas, marca NoShow y finaliza las que ya terminaron
    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void revisarReservas() {
        procesar("liberar reservas vencidas (REQ-18)", ciclo::idsReservadasVencidas, ciclo::liberarPorVencimiento);
        procesar("marcar NoShow (REQ-25)", ciclo::idsConfirmadasSinUso, ciclo::marcarNoShow);
        procesar("finalizar reservas terminadas (REQ-24)", ciclo::idsEnUsoTerminadas, ciclo::finalizarAutomaticamente);
    }

    // REQ-18, REQ-24, REQ-25: cada id va en su propio try/catch para que un error no detenga a los demás
    private void procesar(String tarea, Supplier<List<Long>> buscarIds, Consumer<Long> accion) {
        List<Long> ids;
        try {
            ids = buscarIds.get();
        } catch (Exception e) {
            log.error("Tarea '{}': no se pudo consultar las reservas: {}", tarea, e.getMessage());
            return;
        }
        for (Long id : ids) {
            try {
                accion.accept(id);
            } catch (Exception e) {
                log.error("Tarea '{}': error con la reserva {}: {}", tarea, id, e.getMessage());
            }
        }
    }
}