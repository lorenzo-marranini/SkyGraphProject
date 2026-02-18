package it.unipi.SkyGraph.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AirportRankingDTO {
    private String getIataCode();
    private String getName();
    private Double getScore(); // Questo terrà conto dello score con cui sono restituiti i dati ordinati
}
