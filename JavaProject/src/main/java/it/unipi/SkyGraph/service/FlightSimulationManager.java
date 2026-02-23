package it.unipi.SkyGraph.service;

import it.unipi.SkyGraph.dto.FlightLogDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import java.util.PriorityQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlightSimulationManager implements CommandLineRunner {

    private final CsvFlightLoader loader;
    private final FlightSimulationService simulationService;

    /**
     * Entry point executed at application startup. Loads flight logs from the CSV file
     * into a priority queue and starts the simulation.
     */
    @Override
    public void run(String... args) throws Exception {
        String filePath = "data/logs.csv";

        PriorityQueue<FlightLogDTO> simulationQueue = loader.loadFlightLogs(filePath);

        if (simulationQueue != null && !simulationQueue.isEmpty()) {
            simulationService.startSimulation(simulationQueue);
        } else {
            log.warn("Queue is empty. Check the CSV filepath.");
        }
    }
}
