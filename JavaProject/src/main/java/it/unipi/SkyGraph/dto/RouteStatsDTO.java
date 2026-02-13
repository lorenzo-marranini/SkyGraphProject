package com.skygraph.dto;

public interface RouteStatsDTO {
    String getOrigin();      // Nome aeroporto partenza
    String getDestination(); // Nome aeroporto arrivo
    Double getScore();      // Score
}