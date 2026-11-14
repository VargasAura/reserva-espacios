package co.edu.uis.reservas.scheduler;

import co.edu.uis.reservas.service.CicloReservaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/** Tarea automática que corre cada 5 minutos y envía los recordatorios de reservas confirmadas. */
@Component
public class RecordatorioScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecordatorioScheduler.class);

    private final CicloReservaService ciclo;

    public RecordatorioScheduler(CicloReservaService ciclo) { this.ciclo = ciclo; }

    // REQ-34, REQ-37: busca reservas por empezar y envía el recordatorio (una sola vez por reserva)
    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void enviarRecordatorios() {
        List<Long> ids;
        try {
            ids = ciclo.idsParaRecordatorio();
        } catch (Exception e) {
            log.error("Recordatorios: no se pudo consultar las reservas: {}", e.getMessage());
            return;
        }
        for (Long id : ids) {
            try {
                ciclo.enviarRecordatorio(id);
            } catch (Exception e) {
                log.error("Recordatorios: error con la reserva {}: {}", id, e.getMessage());
            }
        }
    }
}