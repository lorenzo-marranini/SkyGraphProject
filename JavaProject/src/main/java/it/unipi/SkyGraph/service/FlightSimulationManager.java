package it.unipi.SkyGraph.service;

import it.unipi.SkyGraph.dto.FlightLogDTO;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import java.util.PriorityQueue;

@Service
public class FlightSimulationManager implements CommandLineRunner {

    // Teniamo la coda come variabile di stato del servizio
    private final CsvFlightLoader loader;
    private final FlightSimulationService simulationService;

    public FlightSimulationManager(CsvFlightLoader loader, FlightSimulationService simulationService) {
        this.loader = loader;
        this.simulationService = simulationService;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Spring Boot avviato. Inizio caricamento dati CSV...");

        // Specifica il percorso del tuo file.
        // Se lo metti nella cartella root del progetto, basta il nome del file.
        String filePath = "data/logs.csv";

        PriorityQueue<FlightLogDTO> simulationQueue = loader.loadFlightLogs(filePath);

        if (simulationQueue != null && !simulationQueue.isEmpty()) {
            System.out.println("Caricamento completato con successo. Pronti per avviare il timer della simulazione.");
            simulationService.startSimulation(simulationQueue);
        } else {
            System.err.println("Attenzione: La coda è vuota. Controlla il percorso del file CSV.");
        }
    }

}
