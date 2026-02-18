package it.unipi.SkyGraph.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CityStatsDTO {
    private String CityName;
    private String Country;
    private Long TotalFlights;   // Somma dei voli di tutti gli aeroporti della città
    private Integer AirportCount; // Quanti aeroporti ci sono in quella città
}
