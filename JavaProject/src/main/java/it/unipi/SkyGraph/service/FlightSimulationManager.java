package it.unipi.SkyGraph.service;

import it.unipi.SkyGraph.dto.FlightLogDTO;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import java.util.PriorityQueue;

@Service
public class FlightSimulationManager implements CommandLineRunner {

    // Teniamo la coda come variabile di stato del servizio
    private final CsvFlightLoader loader;
    private PriorityQueue<FlightLogDTO> flightQueue;

    public FlightSimulationManager(CsvFlightLoader loader) {
        this.loader = loader;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Spring Boot avviato. Inizio caricamento dati CSV...");

        // Specifica il percorso del tuo file.
        // Se lo metti nella cartella root del progetto, basta il nome del file.
        String filePath = "logs.csv";

        this.flightQueue = loader.loadFlightLogs(filePath);

        if (this.flightQueue != null && !this.flightQueue.isEmpty()) {
            System.out.println("Caricamento completato con successo. Pronti per avviare il timer della simulazione.");
            // TODO: Qui chiameremo il metodo per far partire lo ScheduledExecutorService
        } else {
            System.err.println("Attenzione: La coda è vuota. Controlla il percorso del file CSV.");
        }
    }

    // Metodo getter opzionale se altri servizi avranno bisogno di leggere la coda
    public PriorityQueue<FlightLogDTO> getFlightQueue() {
        return flightQueue;
    }
}
