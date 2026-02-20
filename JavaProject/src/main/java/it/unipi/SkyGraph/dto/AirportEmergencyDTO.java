package it.unipi.SkyGraph.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AirportEmergencyDTO {
    private String id;
    private String name;
    private String city;
    private String country;
    private Double distanceKm;

}
