package it.unipi.SkyGraph.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RouteStatsDTO {
    private String getOrigin();      // Nome aeroporto partenza
    private String getDestination(); // Nome aeroporto arrivo
    private Double getScore();      // Score
}