package it.unipi.SkyGraph.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CityRankDTO {
    private String CityName;
    private String Country;
    private Long TotalFlights;
    private Integer AirportCount;
}