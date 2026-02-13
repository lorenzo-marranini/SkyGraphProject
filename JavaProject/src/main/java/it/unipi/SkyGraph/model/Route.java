package it.unipi.SkyGraph.model;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;
import org.springframework.data.neo4j.core.schema.Property;

@RelationshipProperties
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Route {

    @RelationshipId
    @GeneratedValue
    private Long id;

    @Property("num_flights")
    private Integer numFlights;

    @Property("mean_scheduled_time")
    private Double meanScheduledTime;

    @TargetNode
    private Airport destination;
}