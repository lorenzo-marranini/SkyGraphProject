package it.unipi.SkyGraph.dto;

public interface RouteStatsDTO {
    String getOrigin();      // Nome aeroporto partenza
    String getDestination(); // Nome aeroporto arrivo
    Double getScore();      // Score
}