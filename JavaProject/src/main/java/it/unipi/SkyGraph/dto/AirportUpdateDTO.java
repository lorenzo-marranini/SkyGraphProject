package it.unipi.SkyGraph.dto;

import lombok.Data;

@Data
public class AirportUpdateDTO {
    private String iataCode;
    private String name;
    private Double latitude;
    private Double longitude;
    private String cityName;
    private String country;
}