package it.unipi.SkyGraph.repository;

import it.unipi.SkyGraph.dto.AirlineStatDto;
import it.unipi.SkyGraph.dto.AirportStatDto;
import it.unipi.SkyGraph.model.FlightMongo;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface FlightRepository extends MongoRepository<FlightMongo, String> {

    // Cerca per origine, destinazione e range di data (inizio giornata -> fine giornata)
    @Query("{ 'route.origin.iata': ?0, 'route.destination.iata': ?1, 'flight_info.schedule.departure_datetime': { $gte: ?2, $lt: ?3 } }")
    List<FlightMongo> searchFlights(String origin, String destination, Instant startOfDay, Instant endOfDay);

    // --- QUERY ESISTENTI (1-4) ---
    // (Omesse per brevità, lasciale come erano nel codice precedente)
    @Aggregation(pipeline = {
            "{ '$match': { 'flight_info.schedule.departure_datetime': { '$gte': ?0 } } }",
            "{ '$group': { '_id': '$flight_info.airline.name', 'score': { '$avg': '$stats.tot_delay_minutes' } } }",
            "{ '$sort': { 'score': 1 } }",
            "{ '$project': { '_id': 0, 'airlineName': '$_id', 'score': 1 } }"
    })
    List<AirlineStatDto> findAirlinesByAvgDelay(Instant minDate);

    @Aggregation(pipeline = {
            "{ '$match': { 'flight_info.schedule.departure_datetime': { '$gte': ?0 } } }",
            "{ '$group': { '_id': '$flight_info.airline.name', 'score': { '$sum': 1 } } }",
            "{ '$sort': { 'score': -1 } }",
            "{ '$project': { '_id': 0, 'airlineName': '$_id', 'score': 1 } }"
    })
    List<AirlineStatDto> findAirlinesByTotalFlights(Instant minDate);

    @Aggregation(pipeline = {
            "{ '$match': { 'flight_info.schedule.departure_datetime': { '$gte': ?0 } } }",
            "{ '$group': { '_id': '$flight_info.airline.name', 'score': { '$sum': '$route.distance_km' } } }",
            "{ '$sort': { 'score': -1 } }",
            "{ '$project': { '_id': 0, 'airlineName': '$_id', 'score': 1 } }"
    })
    List<AirlineStatDto> findAirlinesByTotalDistance(Instant minDate);

    @Aggregation(pipeline = {
            "{ '$match': { 'route.origin.iata': ?0, 'route.destination.iata': ?1 } }",
            "{ '$group': { '_id': '$flight_info.airline.name', 'score': { '$avg': '$stats.tot_delay_minutes' } } }",
            "{ '$sort': { 'score': 1 } }",
            "{ '$project': { '_id': 0, 'airlineName': '$_id', 'score': 1 } }"
    })
    List<AirlineStatDto> findAirlinesByRouteDelay(String originIata, String destIata);


    // --- NUOVE QUERY (5-8) ---

    // 5. Compagnie ordinate per deviazioni (is_diverted = 1)
    @Aggregation(pipeline = {
            "{ '$match': { 'flight_info.schedule.departure_datetime': { '$gte': ?0 }, 'stats.is_diverted': 1 } }",
            "{ '$group': { '_id': '$flight_info.airline.name', 'score': { '$sum': 1 } } }", // Conta quante deviazioni
            "{ '$sort': { 'score': -1 } }", // Chi ne ha di più appare prima
            "{ '$project': { '_id': 0, 'airlineName': '$_id', 'score': 1 } }"
    })
    List<AirlineStatDto> findAirlinesByDiverted(Instant minDate);

    // 6. Mean route distance per specific airline (Distanza media per tratta)
    @Aggregation(pipeline = {
            "{ '$match': { 'flight_info.schedule.departure_datetime': { '$gte': ?0 } } }",
            "{ '$group': { '_id': '$flight_info.airline.name', 'score': { '$avg': '$route.distance_km' } } }",
            "{ '$sort': { 'score': -1 } }",
            "{ '$project': { '_id': 0, 'airlineName': '$_id', 'score': 1 } }"
    })
    List<AirlineStatDto> findAirlinesByAvgRouteDistance(Instant minDate);

    // 7. Top aeroporti per ritardi medi (Partenza)
    @Aggregation(pipeline = {
            "{ '$match': { 'flight_info.schedule.departure_datetime': { '$gte': ?0 } } }",
            // Raggruppa per Nome Aeroporto di origine
            "{ '$group': { '_id': '$route.origin.airport_name', 'score': { '$avg': '$stats.tot_delay_minutes' } } }",
            "{ '$sort': { 'score': -1 } }", // Decrescente (i più lenti primi)
            "{ '$limit': 10 }", // Top 10
            "{ '$project': { '_id': 0, 'airportName': '$_id', 'score': 1 } }"
    })
    List<AirportStatDto> findAirportsByAvgDelay(Instant minDate);

    // 8. Airline Efficiency (Km per minuto di volo)
    @Aggregation(pipeline = {
            "{ '$match': { 'flight_info.schedule.departure_datetime': { '$gte': ?0 }, 'stats.air_time_minutes': { '$gt': 0 } } }",
            "{ '$group': { " +
                    "'_id': '$flight_info.airline.name', " +
                    "'totalDist': { '$sum': '$route.distance_km' }, " +
                    "'totalTime': { '$sum': '$stats.air_time_minutes' }" +
                    "} }",
            // Calcola efficienza: Distanza / Tempo
            "{ '$project': { " +
                    "'_id': 0, " +
                    "'airlineName': '$_id', " +
                    "'score': { '$divide': ['$totalDist', '$totalTime'] }" +
                    "} }",
            "{ '$sort': { 'score': -1 } }" // Più km al minuto = Più efficiente
    })
    List<AirlineStatDto> findAirlinesByEfficiency(Instant minDate);
}