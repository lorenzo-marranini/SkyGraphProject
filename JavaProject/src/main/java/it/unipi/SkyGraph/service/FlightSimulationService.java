package it.unipi.SkyGraph.service;

import it.unipi.SkyGraph.config.SimulationClock;
import it.unipi.SkyGraph.dto.FlightLogDTO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.PriorityQueue;

@Slf4j
@Service
@AllArgsConstructor
public class FlightSimulationService {

    private final FlightService flightService;
    private final SimulationClock clock;

    /**
     * Runs the simulation asynchronously. A fresh copy of the original queue is processed
     * each iteration. Flight logs are dispatched in chronological order: all entries whose
     * timestamp does not exceed the current simulated time are flushed and persisted to
     * MongoDB via {@link FlightService#updateFlightLog}. The simulation clock is then
     * advanced by 10 minutes and the loop repeats until the queue is exhausted.
     *
     * @param originalQueue min-heap of flight log entries sorted by timestamp
     */
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

                log.info("Simulated time: {}. Remaining logs in queue: {}", clock.now(), queue.size());
                clock.advanceSimTimeMin(10);
            }
            loop = false;
        }
    }
}
