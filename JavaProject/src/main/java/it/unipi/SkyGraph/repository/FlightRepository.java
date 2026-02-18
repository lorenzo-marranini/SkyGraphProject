package it.unipi.SkyGraph.repository;

import it.unipi.SkyGraph.dto.AirportDTO;
import it.unipi.SkyGraph.dto.AirlineStatDto;
import it.unipi.SkyGraph.dto.AirportStatDto;
import it.unipi.SkyGraph.model.FlightMongo;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.temporal.ChronoUnit;
import java.time.Instant;
import java.util.List;

@Repository
public interface FlightRepository extends MongoRepository<FlightMongo, String> {

    //------------ Guest -----------

    //1) Cerca per origine, destinazione e range di data (inizio giornata -> fine giornata)
    @Query("{ 'route.origin.iata': ?0, 'route.destination.iata': ?1, 'flight_info.schedule.departure_datetime': { $gte: ?2, $lt: ?3 } }")
    List<FlightMongo> searchFlights(String origin, String destination, Instant startOfDay, Instant endOfDay);

    //2)  Cerca per range di data (start -> end) tutti i voli che hanno qualcosa dentro il campo flightlog
    @Query("{ 'flight_info.schedule.departure_datetime': { $gte: ?0, $lt: ?1 }, 'flightlog': { $ne: null } }")
    List<FlightMongo> searchFlightsLive(Instant start, Instant end);

    // ----------------------------- Traffic Controller ------------------

    // --- QUERY ESISTENTI (1-4) ---
    // (Omesse per brevità, lasciale come erano nel codice precedente)

    // 1)
    @Aggregation(pipeline = {
            "{ '$match': { 'flight_info.schedule.departure_datetime': { '$gte': ?0 } } }",
            "{ '$group': { '_id': '$flight_info.airline.name', 'score': { '$avg': '$stats.tot_delay_minutes' } } }",
            "{ '$sort': { 'score': 1 } }",
            "{ '$project': { '_id': 0, 'airlineName': '$_id', 'score': 1 } }"
    })
    List<AirlineStatDto> findAirlinesByAvgDelay(Instant minDate);

    // 2)
    @Aggregation(pipeline = {
            "{ '$match': { 'flight_info.schedule.departure_datetime': { '$gte': ?0 } } }",
            "{ '$group': { '_id': '$flight_info.airline.name', 'score': { '$sum': 1 } } }",
            "{ '$sort': { 'score': -1 } }",
            "{ '$project': { '_id': 0, 'airlineName': '$_id', 'score': 1 } }"
    })
    List<AirlineStatDto> findAirlinesByTotalFlights(Instant minDate);

    // 3)
    @Aggregation(pipeline = {
            "{ '$match': { 'flight_info.schedule.departure_datetime': { '$gte': ?0 } } }",
            "{ '$group': { '_id': '$flight_info.airline.name', 'score': { '$sum': '$route.distance_km' } } }",
            "{ '$sort': { 'score': -1 } }",
            "{ '$project': { '_id': 0, 'airlineName': '$_id', 'score': 1 } }"
    })
    List<AirlineStatDto> findAirlinesByTotalDistance(Instant minDate);

    // 4)
    @Aggregation(pipeline = {
            "{ '$match': { 'flight_info.schedule.departure_datetime': { '$gte': ?0 } } }",
            "{ '$group': { '_id': '$route.origin.name', 'score': { '$sum': 1 } } }",
            "{ '$sort': { 'score': -1 } }",
            "{ '$project': { '_id': 0, 'airportName': '$_id', 'score': 1 } }"
    })
    List<AirportStatDto> findBusiestAirports(Instant minDate);

    // 5) Su Neo4j in AirportRepository senza intervallo di tempo
    // 5) Fatta su Mongo con l'intervallo di tempo

    @Aggregation(pipeline = {
            "{ '$match': { 'flight_info.schedule.departure_datetime': { '$gte': ?0 } } }",
            "{ '$group': { '_id': { 'origin': '$route.origin.iata', 'dest': '$route.destination.iata' } } }",
            "{ '$group': { '_id': '$_id.origin', 'score': { '$sum': 1 } } }",
            "{ '$sort': { 'score': -1 } }",
            "{ '$project': { '_id': 0, 'airportCode': '$_id', 'score': 1 } }"
    })
    List<AirportStatDto> findAirportConnections(Instant minDate);

    //6)
    @Aggregation(pipeline = {
            "{ '$match': { 'route.origin.iata': ?0, 'route.destination.iata': ?1, 'flight_info.schedule.departure_datetime': { '$gte': ?2 } } }",
            "{ '$group': { '_id': '$flight_info.airline.name', 'score': { '$sum': 1 } } }",
            "{ '$sort': { 'score': -1 } }",
            "{ '$project': { '_id': 0, 'airlineName': '$_id', 'score': 1 } }"
    })
    List<AirlineStatDto> findAirlinesFlightsByRoute(String origin, String destination, Instant minDate);


    // 7)
    @Aggregation(pipeline = {
            "{ '$match': { 'route.origin.iata': ?0, 'route.destination.iata': ?1, 'flight_info.schedule.departure_datetime': { '$gte': ?2 } } }",
            "{ '$group': { '_id': '$flight_info.airline.name', 'score': { '$avg': '$stats.tot_delay_minutes' } } }",
            "{ '$sort': { 'score': 1 } }",
            "{ '$project': { '_id': 0, 'airlineName': '$_id', 'score': 1 } }"
    })
    List<AirlineStatDto> findAirlinesByRouteDelay(String originIata, String destIata, Instant minDate);

    //8) da fare emergency landing


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