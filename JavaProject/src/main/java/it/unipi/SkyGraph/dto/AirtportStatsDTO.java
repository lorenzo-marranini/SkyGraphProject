package com.skygraph.dto;

public interface AirportRankingDTO {
    String getIataCode();
    String getName();
    Double getScore(); // Questo terrà conto dello score con cui sono restituiti i dati ordinati
}

public interface AirportDTO {
    String getIataCode();
    String getName();
}

