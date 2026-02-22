package it.unipi.SkyGraph.repository;

import it.unipi.SkyGraph.model.FlightMongo;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FlightRepositoryCustom {
    void updateFlightLogByKey(String flightKey, FlightMongo.FlightLog log);
}
