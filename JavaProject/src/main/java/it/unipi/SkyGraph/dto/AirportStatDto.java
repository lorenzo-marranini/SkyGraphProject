package it.unipi.SkyGraph.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AirportStatDto {
    private String airportName;
    private double score; // Usato per ritardo medio, etc.
}