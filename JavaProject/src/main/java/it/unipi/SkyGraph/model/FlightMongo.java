package it.unipi.SkyGraph.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "flights")
public class FlightMongo {

    @Id
    private String id;

    @Field("flight_info")
    private FlightInfo flightInfo;

    private Route route;

    private Stats stats;

    @Field("flight_log")
    private FlightLog flightLog;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlightInfo {
        @Field("flight_key")
        private String flightKey;

        private Airline airline;

        private Schedule schedule;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Airline {
        private String iata;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Schedule {
        @Field("duration_minutes")
        private Integer durationMinutes;

        @Field("departure_datetime")
        private Instant departureDatetime;

        @Field("arrival_datetime")
        private Instant arrivalDatetime;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Route {
        private AirportDetails origin;

        private AirportDetails destination;

        @Field("distance_km")
        private Double distanceKm;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AirportDetails {
        private String iata;

        @Field("airport_name")
        private String airportName;

        private String city;
        private String state;
        private String country;

        private GeoLocation location;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeoLocation {
        private String type; // "Point"
        private List<Double> coordinates; // [Long, Lat]
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Stats {
        @Field("tot_delay_minutes")
        private Integer totalDelayMinutes;

        @Field("is_cancelled")
        private Integer isCancelled;

        @Field("is_diverted")
        private Integer isDiverted;

        @Field("air_time_minutes")
        private Integer airTimeMinutes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlightLog {

        @Field("live_location")
        private GeoLocation location;
        private Integer altitude;
        private Integer speed;
        private Instant timestamp;
        private Instant eta;
    }
}