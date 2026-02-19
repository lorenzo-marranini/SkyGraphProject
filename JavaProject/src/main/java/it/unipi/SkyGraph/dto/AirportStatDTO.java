package it.unipi.SkyGraph.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
public class AirportStatDTO {
    private String iataCode;
    private String name;
    private String city;
    private Double score; // Questo terrà conto dello score con cui sono restituiti i dati ordinati
    private String scoreType;
}