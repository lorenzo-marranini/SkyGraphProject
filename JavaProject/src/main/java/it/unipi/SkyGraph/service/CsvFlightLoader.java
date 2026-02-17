package it.unipi.SkyGraph.service;

import it.unipi.SkyGraph.dto.FlightLogDTO;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.PriorityQueue;

@Component
public class CsvFlightLoader {

    public PriorityQueue<FlightLogDTO> loadFlightLogs(String filePath) {
        PriorityQueue<FlightLogDTO> flightQueue = new PriorityQueue<>();
        Path path = Paths.get(filePath);

        try (BufferedReader br = Files.newBufferedReader(path)) {
            String line;
            boolean isFirstLine = true;
            int lineNumber = 0;

            while ((line = br.readLine()) != null) {
                lineNumber++;

                // Rimuove l'eventuale BOM invisibile sulla primissima riga
                if (isFirstLine) {
                    line = line.replace("\uFEFF", "");
                    isFirstLine = false;
                    continue;
                }

                String[] values = line.split(",");

                // Ora abbiamo 11 colonne, quindi controlliamo che ci siano abbastanza dati
                if (values.length < 11) continue;

                try {
                    FlightLogDTO logDTO = new FlightLogDTO(
                            values[0].trim(),                                // flight
                            Double.parseDouble(values[1].trim()),            // lat
                            Double.parseDouble(values[2].trim()),            // lon
                            Integer.parseInt(values[3].trim()),              // alt
                            Integer.parseInt(values[4].trim()),              // gspeed
                            Integer.parseInt(values[5].trim()),              // vspeed
                            parseInstantSafe(values[6]),                     // timestamp
                            values[7].trim(),                                // orig_iata
                            values[8].trim(),                                // dest_iata
                            parseInstantSafe(values[9]),                     // eta
                            values[10].trim()                                // flight_key (NUOVO)
                    );

                    // Inseriamo solo se il timestamp fondamentale è presente
                    if (logDTO.getTimestamp() != null) {
                        flightQueue.add(logDTO);
                    }

                } catch (Exception parseEx) {
                    // Questo ci dice ESATTAMENTE quale riga del CSV sta fallendo e perché
                    System.err.println("Errore di formato alla riga " + lineNumber + ": " + parseEx.getMessage());
                }
            }
            System.out.println("Caricati " + flightQueue.size() + " log di volo in memoria.");

        } catch (IOException e) {
            System.err.println("Errore nella lettura del file CSV: " + e.getMessage());
        }

        return flightQueue;
    }

    // Metodo di utilità per gestire stringhe vuote o malformate
    private Instant parseInstantSafe(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return Instant.parse(dateStr.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Impossibile parsare la data: '" + dateStr + "'", e);
        }
    }
}
