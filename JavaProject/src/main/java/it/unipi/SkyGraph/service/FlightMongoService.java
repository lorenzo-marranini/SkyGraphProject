package it.unipi.SkyGraph.service;

import it.unipi.SkyGraph.dto.FlightLogDTO;
import it.unipi.SkyGraph.model.FlightMongo;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FlightMongoService {

    private final MongoTemplate mongoTemplate;

    public FlightMongoService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void updateFlightLog(FlightLogDTO dto) {

        Query query = new Query(
                Criteria.where("flight_info.flight_key")
                        .is(dto.getFlightKey())
        );

        FlightMongo.FlightLog log = new FlightMongo.FlightLog(
                new FlightMongo.GeoLocation(
                        "Point",
                        List.of(dto.getLon(), dto.getLat())
                ),
                dto.getAlt(),
                dto.getGspeed(),
                dto.getTimestamp(),
                dto.getEta()
        );

        Update update = new Update()
                .set("flight_log", log);

        mongoTemplate.updateFirst(query, update, FlightMongo.class);
    }
}
