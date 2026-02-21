package it.unipi.SkyGraph.service;

import it.unipi.SkyGraph.config.SimulationClock;
import it.unipi.SkyGraph.dto.FlightLogDTO;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.PriorityQueue;

@Service
@AllArgsConstructor
public class FlightSimulationService {

    private final FlightService flightService;
    private final SimulationClock clock;


    @Async
    public void startSimulation(PriorityQueue<FlightLogDTO> originalQueue) {
        boolean loop = true;
        while (loop) {

            PriorityQueue<FlightLogDTO> queue = new PriorityQueue<>(originalQueue);

            while (!queue.isEmpty()) {
                FlightLogDTO nextFlight = queue.peek();

                while (nextFlight != null && !nextFlight.getTimestamp().isAfter(clock.now())) {
                    FlightLogDTO processed = queue.poll();

                    if (processed == null)
                        break;
                    // update mongo db
                    flightService.updateFlightLog(processed);
                    nextFlight = queue.peek();
                }

                clock.advanceSimTimeMin(10);
            }
            loop = false;
        }
    }
}
