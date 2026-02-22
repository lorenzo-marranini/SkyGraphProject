package it.unipi.SkyGraph.repository;

import com.mongodb.WriteConcern;
import it.unipi.SkyGraph.model.FlightMongo;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

public class FlightRepositoryCustomImpl implements FlightRepositoryCustom {
    @Autowired
    private MongoTemplate mongoTemplate;

    // custom update method to set the flight log for a specific flight key
    // uses write concern W1 to ensure availability while allowing for eventual consistency
    @Override
    public void updateFlightLogByKey(String flightKey, FlightMongo.FlightLog log) {
        Query query = new Query(Criteria.where("flight_info.flight_key").is(flightKey));

        mongoTemplate.execute(FlightMongo.class, collection -> {
            var converter = mongoTemplate.getConverter();

            Document logDocument = new Document();
            converter.write(log, logDocument);

            Document queryDoc = query.getQueryObject();
            Document updateDoc = new Document("$set", new Document("flight_log", logDocument));

            collection.withWriteConcern(WriteConcern.W1)
                    .updateOne(queryDoc, updateDoc);

            return null;
        });
    }
}
