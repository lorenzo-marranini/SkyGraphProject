package it.unipi.SkyGraph.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AirlineReportDTO {
    private String airlineIata;
    private double totalKm;       // Totale KM percorsi
    private double avgKm;         // Media KM per volo
    private double avgDelay;      // Media ritardo (minuti)
    private double efficiency;    // Km per minuto (Velocità media commerciale)
    private long totalFlights;    // Numero totale voli
}
