package it.unipi.SkyGraph.repository;

import it.unipi.SkyGraph.dto.AirlineStatDto;
import it.unipi.SkyGraph.model.FlightMongo;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlightRepository extends MongoRepository<FlightMongo, String> {

    // 1. Compagnie ordinate per ritardo medio
    @Aggregation(pipeline = {
            "{ '$match': { 'flight_info.date': { '$gte': ?0 } } }",
            "{ '$group': { '_id': '$flight_info.airline_name', 'score': { '$avg': '$stats.tot_delay' } } }",
            "{ '$sort': { 'score': 1 } }",
            "{ '$project': { '_id': 0, 'airlineName': '$_id', 'score': 1 } }"
    })
    List<AirlineStatDto> findAirlinesByAvgDelay(String minDate);

    // 2. Compagnie ordinate per numero di voli totali
    @Aggregation(pipeline = {
            "{ '$match': { 'flight_info.date': { '$gte': ?0 } } }",
            "{ '$group': { '_id': '$flight_info.airline_name', 'score': { '$sum': 1 } } }",
            "{ '$sort': { 'score': -1 } }",
            "{ '$project': { '_id': 0, 'airlineName': '$_id', 'score': 1 } }"
    })
    List<AirlineStatDto> findAirlinesByTotalFlights(String minDate);

    // 3. Compagnie ordinate per km percorsi
    @Aggregation(pipeline = {
            "{ '$match': { 'flight_info.date': { '$gte': ?0 } } }",
            "{ '$group': { '_id': '$flight_info.airline_name', 'score': { '$sum': '$route.distance' } } }",
            "{ '$sort': { 'score': -1 } }",
            "{ '$project': { '_id': 0, 'airlineName': '$_id', 'score': 1 } }"
    })
    List<AirlineStatDto> findAirlinesByTotalDistance(String minDate);

    // 4. Data una rotta, compagnie ordinate per numero di voli (dal più alto al più basso)
    @Aggregation(pipeline = {
            "{ '$match': { 'route.origin.iata': ?0, 'route.destination.iata': ?1 } }",
            "{ '$group': { '_id': '$flight_info.airline_name', 'score': { '$sum': 1 } } }",
            "{ '$sort': { 'score': -1 } }",
            "{ '$project': { '_id': 0, 'airlineName': '$_id', 'score': 1 } }"
    })
    List<AirlineStatDto> findAirlinesByRouteFlights(String originIata, String destIata);

    // 5. Data una rotta, compagnie ordinate per ritardo medio (dal più piccolo al più grande)
    @Aggregation(pipeline = {
            "{ '$match': { 'route.origin.iata': ?0, 'route.destination.iata': ?1 } }",
            "{ '$group': { '_id': '$flight_info.airline_name', 'score': { '$avg': '$stats.tot_delay' } } }",
            "{ '$sort': { 'score': 1 } }",
            "{ '$project': { '_id': 0, 'airlineName': '$_id', 'score': 1 } }"
    })
    List<AirlineStatDto> findAirlinesByRouteDelay(String originIata, String destIata);

}