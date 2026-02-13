package it.unipi.SkyGraph.dto;

public interface AirportRankingDTO {
    String getIataCode();
    String getName();
    Double getScore(); // Questo terrà conto dello score con cui sono restituiti i dati ordinati
}
