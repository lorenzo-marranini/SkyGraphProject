package it.unipi.SkyGraph.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CityStatsDTO {
    private String CityName;
    private String StateId;
    private Long TotalDepartures;
    private Long TotalArrivals;
    private Long TotalFlights;
    private Integer AirportCount;
}