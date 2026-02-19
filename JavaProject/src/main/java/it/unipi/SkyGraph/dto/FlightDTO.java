package it.unipi.SkyGraph.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FlightDTO {

    private String flightId;
    private Airline airline;
    private Route route;
    private Schedule schedule;
    private Status status;
    private Metrics metrics;
    private Position currentPosition;

    @Data
    @Builder
    public static class Airline {
        private String iata;
        private String name;
    }

    @Data
    @Builder
    public static class Route {
        private Airport origin;
        private Airport destination;
    }

    @Data
    @Builder
    public static class Airport {
        private String iata;
        private String city;
    }

    @Data
    @Builder
    public static class Schedule {
        private Instant departureUtc;
        private Instant arrivalUtc;
        private Integer durationMinutes;
    }

    @Data
    @Builder
    public static class Status {
        private boolean cancelled;
        private boolean diverted;
        private Integer delayMinutes;
    }

    @Data
    @Builder
    public static class Metrics {
        private Double distanceKm;
        private Integer speedKmh;
    }

    @Data
    @Builder
    public static class Position {
        private Double latitude;
        private Double longitude;
    }
}
