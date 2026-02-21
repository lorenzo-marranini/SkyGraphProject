package it.unipi.SkyGraph.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class FlightCreateDTO {
    // Airline
    private String airlineIata;
    private String airlineName;

    // Schedule
    private Integer durationMinutes;
    private Instant departureDatetime;
    private Instant arrivalDatetime;

    // Route
    private String originIata;
    private String destinationIata;

    // Stats
    private Integer totDelayMinutes;
    private Integer isCancelled;
    private Integer isDiverted;
    private Integer airTimeMinutes;
}