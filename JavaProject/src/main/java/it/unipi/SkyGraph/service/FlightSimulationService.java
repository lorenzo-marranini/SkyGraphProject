package it.unipi.SkyGraph.service;

import it.unipi.SkyGraph.dto.FlightLogDTO;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.PriorityQueue;

@Service
public class FlightSimulationService {

    private final FlightMongoService flightMongoService;

    public FlightSimulationService(FlightMongoService flightMongoService) {
        this.flightMongoService = flightMongoService;
    }

    @Async
    public void startSimulation(PriorityQueue<FlightLogDTO> originalQueue) {
        // 1. Verifica se la coda originale ha elementi all'inizio
        System.out.println("Elementi totali caricati per la simulazione: " + originalQueue.size());

        while (true) {

            // RESET timestamp
            LocalDate date = LocalDate.of(2026, 2, 25);
            Instant currentTimeStamp = date.atTime(11, 50).toInstant(ZoneOffset.UTC);

            // COPIA: Assicurati che nessuno tocchi 'originalQueue' all'esterno
            PriorityQueue<FlightLogDTO> queue = new PriorityQueue<>(originalQueue);

            System.out.println("--- Avvio ciclo su " + queue.size() + " voli ---");

            while (!queue.isEmpty()) {
                FlightLogDTO nextFlight = queue.peek();

                while (nextFlight != null && !nextFlight.getTimestamp().isAfter(currentTimeStamp)) {
                    FlightLogDTO processed = queue.poll();

                    // update the mongo db
                    flightMongoService.updateFlightLog(processed);

                    nextFlight = queue.peek();
                }

                currentTimeStamp = currentTimeStamp.plus(10, ChronoUnit.MINUTES);
                try { Thread.sleep(2000); } catch (InterruptedException e) { return; }
            }

            System.out.println("Ciclo terminato. La coda originale ha ancora " + originalQueue.size() + " elementi.");
            try { Thread.sleep(2000); } catch (InterruptedException e) { return; }
        }
    }
}
