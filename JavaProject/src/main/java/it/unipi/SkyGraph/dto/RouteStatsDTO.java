package it.unipi.SkyGraph.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RouteStatsDTO {
    private String Origin;      // Nome aeroporto partenza
    private String Destination; // Nome aeroporto arrivo
    private Double Score;      // Score
}