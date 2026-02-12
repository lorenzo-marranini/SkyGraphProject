package it.unipi.SkyGraph.model;


import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "flights")
@Data

public class FlightMongo {

}
