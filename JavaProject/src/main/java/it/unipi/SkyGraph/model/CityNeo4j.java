package it.unipi.SkyGraph.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Node("City")
@Data // Genera getter, setter, toString, equals, hashCode
@NoArgsConstructor
@AllArgsConstructor
public class City {

    @Id
    @Property("city_state")
    private String cityState;

    @Property("name") // Nel Cypher abbiamo fatto: SET c.name = row.city
    private String name;

    @Property("state_id")
    private String stateId;

    @Property("state_name")
    private String stateName;

    @Property("population")
    private Integer population; // Era toInteger nel Cypher

    @Property("density")
    private Double density;    // Era toFloat nel Cypher

    @Property("timezone")
    private String timezone;

    @Property("country")
    private String country;
}