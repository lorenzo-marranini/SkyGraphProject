package it.unipi.SkyGraph.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "flight")
public class FlightMongo {

    @Id
    private String id;

    @Field("flight_info")
    private FlightInfo flightInfo;

    private Route route;
    private Stats stats;

    @Field("flight_log")
    private Object flightLog;

    @Data
    @NoArgsConstructor
    public static class FlightInfo {
        @Field("airline_code") private String airlineCode;
        @Field("airline_name") private String airlineName;
        @Field("flight_number") private String flightNumber;
        private String date;
        @Field("scheduled_departure") private String scheduledDeparture;
        @Field("scheduled_arrival") private String scheduledArrival;
    }

    @Data
    @NoArgsConstructor
    public static class Route {
        private AirportInfo origin;
        private AirportInfo destination;
        private Integer distance;
    }

    @Data
    @NoArgsConstructor
    public static class AirportInfo {
        private String iata;
        @Field("airport_name") private String airportName;
        private String city;
        private Location location;
    }

    @Data
    @NoArgsConstructor
    public static class Location {
        private String type;
        private List<Double> coordinates;
    }

    @Data
    @NoArgsConstructor
    public static class Stats {
        @Field("tot_delay") private Integer totDelay;
        private String cancelled;
        private String diverted;
        @Field("air_time") private Integer airTime;
    }
}