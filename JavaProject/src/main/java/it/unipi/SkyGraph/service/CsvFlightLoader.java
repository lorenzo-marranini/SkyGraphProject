package it.unipi.SkyGraph.service;

import it.unipi.SkyGraph.dto.FlightLogDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.PriorityQueue;

@Slf4j
@Component
public class CsvFlightLoader {

    /**
     * Parses the CSV at the given path into a priority queue of {@link FlightLogDTO}.
     * The header line is skipped; rows with fewer than 11 columns or a null timestamp are discarded.
     *
     * @param filePath absolute or relative path to the CSV file
     * @return a {@link PriorityQueue} of flight logs sorted by natural order (timestamp)
     */
    public PriorityQueue<FlightLogDTO> loadFlightLogs(String filePath) {
        PriorityQueue<FlightLogDTO> flightQueue = new PriorityQueue<>();
        Path path = Paths.get(filePath);

        try (BufferedReader br = Files.newBufferedReader(path)) {
            String line;
            boolean isFirstLine = true;
            int lineNumber = 0;

            while ((line = br.readLine()) != null) {
                lineNumber++;

                // Strip BOM and skip header
                if (isFirstLine) {
                    line = line.replace("\uFEFF", "");
                    isFirstLine = false;
                    continue;
                }

                String[] values = line.split(",");

                // Check for enough data?
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
                    if (logDTO.getTimestamp() != null) {
                        flightQueue.add(logDTO);
                    }

                } catch (Exception parseEx) {
                    log.warn("Format error at line {}: {}", lineNumber, parseEx.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("Failed to read CSV file: {}", e.getMessage(), e);
        }

        return flightQueue;
    }

    /**
     * Parses an ISO-8601 timestamp string, returning {@code null} for blank input.
     *
     * @throws IllegalArgumentException if the string is non-empty but not a valid instant
     */
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
