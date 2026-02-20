package it.unipi.SkyGraph.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CityRankDTO {
    private String CityName;
    private String StateId;
    private Long TotalFlights;
    private Integer AirportCount;
}