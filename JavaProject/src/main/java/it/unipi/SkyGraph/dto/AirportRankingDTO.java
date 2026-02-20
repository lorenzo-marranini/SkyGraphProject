package it.unipi.SkyGraph.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AirportRankingDTO {
    private String iata;
    private String name;
    private Double score; // Questo terrà conto dello score con cui sono restituiti i dati ordinati
    private String scoreType;
}
