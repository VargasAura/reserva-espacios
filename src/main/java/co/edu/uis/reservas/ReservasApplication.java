package co.edu.uis.reservas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // necesario para las tareas automáticas (REQ-18, REQ-25, REQ-34)
public class ReservasApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReservasApplication.class, args);
    }
}