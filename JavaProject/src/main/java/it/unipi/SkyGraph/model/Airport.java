package it.unipi.SkyGraph.model;

import lombok.Data; // Importiamo Lombok
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;
import java.util.ArrayList;
import java.util.List;

@Node("Airport")
@Data
public class Airport {

    @Id
    @Property("iata_code")
    private String iataCode;

    @Property("name")
    private String name;

    @Property("latitude")
    private Double latitude;

    @Property("longitude")
    private Double longitude;


    // Relationships
    @Relationship(type = "LOCATED_IN", direction = Relationship.Direction.OUTGOING)
    private City city;

    @Relationship(type = "ROUTE", direction = Relationship.Direction.OUTGOING)
    private List<Route> routes = new ArrayList<>();

}