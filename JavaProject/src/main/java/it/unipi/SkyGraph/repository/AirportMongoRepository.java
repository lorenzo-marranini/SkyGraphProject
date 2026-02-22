package it.unipi.SkyGraph.repository;

import it.unipi.SkyGraph.dto.AirportEmergencyDTO;
import org.springframework.data.mongodb.repository.Aggregation;

import java.util.List;
import it.unipi.SkyGraph.dto.*;
import it.unipi.SkyGraph.model.FlightMongo;
import it.unipi.SkyGraph.model.AirportMongo;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.util.Optional;


public interface AirportMongoRepository extends MongoRepository<AirportMongo, String> {
    // TC7
    @Aggregation(pipeline = {
            """
            {
                $geoNear: {
                    near: { type: 'Point', coordinates: [ ?0, ?1 ] },
                    key: 'location',
                    distanceField: 'distanceKm',
                    spherical: true,
                    distanceMultiplier: 0.001
                }
            }
            """,
            "{ $limit: 5 }",
            """
            {
                $project: {
                    _id: 1,
                    name: 1,
                    city: 1,
                    country: 1,
                    distanceKm: 1
                }
            }
            """
    })
    List<AirportEmergencyDTO> findNearestAirports(double longitude, double latitude);
}
