package it.unipi.SkyGraph.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Data
@NoArgsConstructor
@Document(collection = "airport")
public class AirportMongo {

    @Id
    private String id;
    private String name;
    private String city;
    private String state;
    private String country;
    private Location location;

    @Data
    @NoArgsConstructor
    public static class Location {
        private String type;
        private List<Double> coordinates;
    }
}