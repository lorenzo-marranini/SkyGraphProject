package it.unipi.SkyGraph.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
public class AirportRankingDTO {
    private String IataCode;
    private String Name;
    private Double Score; // Questo terrà conto dello score con cui sono restituiti i dati ordinati
    private String scoreType;
}
