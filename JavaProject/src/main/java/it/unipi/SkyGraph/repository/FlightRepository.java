package it.unipi.SkyGraph.repository;

import it.unipi.SkyGraph.dto.*;
import it.unipi.SkyGraph.model.FlightMongo;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import java.time.temporal.ChronoUnit;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlightRepository extends MongoRepository<FlightMongo, String> {



    //----------------------------- GUEST--------------------------------------

    // G1 Cerca per origine, destinazione e range di data (inizio giornata -> fine giornata)
    @Query("{ 'route.origin.iata': ?0, 'route.destination.iata': ?1, 'flight_info.schedule.departure_datetime': { $gte: ?2, $lt: ?3 } }")
    List<FlightMongo> searchFlights(String origin, String destination, Instant startOfDay, Instant endOfDay);

    // G2 Cerca per range di data (start -> end) tutti i voli che hanno qualcosa dentro il campo fligh_tlog
    @Query("{ 'flight_info.schedule.departure_datetime': { $gte: ?0, $lt: ?1 }, 'flight_log': { $ne: null } }")
    List<FlightMongo> searchFlightsLive(Instant start, Instant end);

    // -------------------------------- TRAFFIC CONTROLLER ------------------------------

    // TC1 Restituisce le airlines ordinate per AVG delay
    @Aggregation(pipeline = {
            "{ '$match': { " +
                    "'flight_info.schedule.departure_datetime': { '$gte': ?0, '$lte': ?1 }, " +
                    "'stats.tot_delay_minutes': { '$ne': null } " +
                    "} }",
            "{ '$group': { '_id': '$flight_info.airline.name', 'score': { '$avg': '$stats.tot_delay_minutes' } } }",
            "{ '$sort': { 'score': 1 } }",
            "{ '$project': { '_id': 0, 'airlineName': '$_id', 'score': 1 } }"
    })
    List<AirlineStatDTO> findAirlinesByAvgDelay(Instant start, Instant end);

    // TC2) Restituisce le airlines ordinate per numero di voli
    @Aggregation(pipeline = {
            "{ '$match': { 'flight_info.schedule.departure_datetime': { $gte: ?0, $lt: ?1 } } }",
            "{ '$group': { '_id': '$flight_info.airline.name', 'score': { '$sum': 1 } } }",
            "{ '$sort': { 'score': -1 } }",
            "{ '$project': { '_id': 0, 'airlineName': '$_id', 'score': 1 } }"
    })
    List<AirlineStatDTO> findAirlinesByTotalFlights(Instant start, Instant end);

    // TC3 Restituisce le airlines ordinate per km volati
    @Aggregation(pipeline = {
            "{ '$match': { 'flight_info.schedule.departure_datetime': { '$gte': ?0, '$lte': ?1 } } }",
            "{ '$group': { '_id': '$flight_info.airline.name', 'score': { '$sum': '$route.distance_km' } } }",
            "{ '$sort': { 'score': -1 } }",
            "{ '$project': { '_id': 0, 'airlineName': '$_id', 'score': 1 } }"
    })
    List<AirlineStatDTO> findAirlinesByTotalDistance(Instant start, Instant end);

    // TC4  Restituisce le airlines ordinate per AVG KM percorsi in volo
    @Aggregation(pipeline = {
            "{ '$match': { 'flight_info.schedule.departure_datetime': { '$gte': ?0, '$lte': ?1 } } }",
            "{ '$group': { '_id': '$flight_info.airline.name', 'score': { '$avg': '$route.distance_km' } } }",
            "{ '$sort': { 'score': -1 } }",
            "{ '$project': { '_id': 0, 'airlineName': '$_id', 'score': 1 } }"
    })
    List<AirlineStatDTO> findAirlinesByAvgRouteDistance(Instant start, Instant end);


    // TC5 Restituiscce le airlines ordinate per numero di voli su una specifica rotta ( origin -> destination )
    @Aggregation(pipeline = {
            "{ '$match': { 'route.origin.iata': ?0, 'route.destination.iata': ?1, 'flight_info.schedule.departure_datetime': { '$gte': ?2, '$lte': ?3 } } }",
            "{ '$group': { '_id': '$flight_info.airline.name', 'score': { '$sum': 1 } } }",
            "{ '$sort': { 'score': -1 } }",
            "{ '$project': { '_id': 0, 'airlineName': '$_id', 'score': 1 } }"
    })
    List<AirlineStatDTO> findAirlinesByRoute(String origin, String destination, Instant start, Instant end);

    // TC6 Restituisce le airlines ordinate per AVG Delay su una specifica rotta ( origin -> destination )
    @Aggregation(pipeline = {
            "{ '$match': { 'route.origin.iata': ?0, 'route.destination.iata': ?1, 'flight_info.schedule.departure_datetime': { '$gte': ?2, '$lte': ?3 }, 'stats.tot_delay_minutes': { '$ne': null } } }",
            "{ '$group': { '_id': '$flight_info.airline.name', 'score': { '$avg': '$stats.tot_delay_minutes' } } }",
            "{ '$sort': { 'score': 1 } }",
            "{ '$project': { '_id': 0, 'airlineName': '$_id', 'score': 1 } }"
    })
    List<AirlineStatDTO> findAirlinesByRouteDelay(String originIata, String destIata, Instant start, Instant end);

    // TC7 Restituisce data una flightkey, gli aeroporti ordinati per distanza dal punto geografico in cui si trova adesso il volo, solo se è in volo.

    @Query("{ 'flight_info.flight_key': ?0, 'flight_log': { $ne: null } }")
    Optional<FlightMongo> findLiveFlightByKey(String flightKey);



    // TC8 Dato un aeroporto chiuso, trovare un altro aeroporto che abbia il piu alto rapporto tra connessioni in comune fratto distanza
    // Fatta su Neo4j in AirportRepository



    //------------------------AIRLINE REPRESENTATIVE-----------------------------

    // AR1
    @Query(value = "{ 'route.origin.iata': ?0, 'route.destination.iata': ?1, 'flight_info.schedule.departure_datetime': { $gt: ?2 } }",
            sort = "{ 'flight_info.schedule.departure_datetime': 1 }")
    List<FlightMongo> findNextFlight(String origin, String dest, Instant minTime, Pageable pageable);


    // AR2 Restituisce le rotte con piu voli nell'intervallo di tempo
    @Aggregation(pipeline = {
            "{ '$match': { 'flight_info.schedule.departure_datetime': { '$gte': ?0, '$lte': ?1 } } }",
            "{ '$group': { " +
                    "'_id': { " +
                    "'origin': '$route.origin.iata', " +
                    "'destination': '$route.destination.iata' " +
                    "}, " +
                    "'count': { '$sum': 1 } " +
                    "} }",
            "{ '$sort': { 'count': -1 } }",
            "{ '$limit': 15 }",
            "{ '$project': { " +
                    "'_id': 0, " +
                    "'Origin': '$_id.origin', " +
                    "'Destination': '$_id.destination', " +
                    "'Score': '$count' " +
                    "} }"
    })
    List<RouteStatsDTO> findRoutesByFlightCount(Instant start, Instant end);

    // AR2.1 Restituisce le rotte ordinate per numero di voli cancellati
    @Aggregation(pipeline = {
            "{ '$match': { " +
                    "'flight_info.schedule.departure_datetime': { '$gte': ?0, '$lte': ?1 }, " +
                    "'stats.is_cancelled': 1 " +
                    "} }",
            "{ '$group': { " +
                    "'_id': { " +
                    "'origin': '$route.origin.iata', " +
                    "'destination': '$route.destination.iata' " +
                    "}, " +
                    "'count': { '$sum': 1 } " +
                    "} }",
            "{ '$sort': { 'count': -1 } }",
            "{ '$limit': 15 }",
            "{ '$project': { " +
                    "'_id': 0, " +
                    "'Origin': '$_id.origin', " +
                    "'Destination': '$_id.destination', " +
                    "'Score': '$count' " +
                    "} }"
    })
    List<RouteStatsDTO> findRoutesByCancelledCount(Instant start, Instant end);

    // AR2.2 Restituisce le rotte ordinate per numero di voli deviati
    @Aggregation(pipeline = {
            "{ '$match': { " +
                    "'flight_info.schedule.departure_datetime': { '$gte': ?0, '$lte': ?1 }, " +
                    "'stats.is_diverted': 1 " +
                    "} }",
            "{ '$group': { " +
                    "'_id': { " +
                    "'origin': '$route.origin.iata', " +
                    "'destination': '$route.destination.iata' " +
                    "}, " +
                    "'count': { '$sum': 1 } " +
                    "} }",
            "{ '$sort': { 'count': -1 } }",
            "{ '$limit': 15 }",
            "{ '$project': { " +
                    "'_id': 0, " +
                    "'Origin': '$_id.origin', " +
                    "'Destination': '$_id.destination', " +
                    "'Score': '$count' " +
                    "} }"
    })
    List<RouteStatsDTO> findRoutesByDivertedCount(Instant start, Instant end);

    // AR3 Visualizzare gli aeroporti ordinati per il betweenness centrality score
    // NEO4J FATTA SU AirportRepository

    // AR4 Visualizzare gli aeroporti ordinati per il betweenness centrality score
    // NEO4J FATTA SU AirportRepository

    // AR5 Restituisce gli aeroporti ordinati per ritardo medio
    @Aggregation(pipeline = {
            "{ '$match': { " +
                    "'flight_info.schedule.departure_datetime': { '$gte': ?0, '$lte': ?1 }, " +
                    "'stats': { '$ne': null } " +
                    "} }",
            "{ '$group': { " +
                    "'_id': '$route.origin.airport_name', " +
                    "'iataCode': { '$first': '$route.origin.iata' }, " +
                    "'city': { '$first': '$route.origin.city' }, " +
                    "'score': { '$avg': '$stats.tot_delay_minutes' } " +
                    "} }",
            "{ '$sort': { 'score': -1 } }",
            "{ '$project': { " +
                    "'_id': 0, " +
                    "'name': '$_id', " +      // Spostiamo l'id del gruppo nel campo 'name'
                    "'iataCode': 1, " +
                    "'city': 1, " +
                    "'score': 1 " +
                    "} }"
    })
    List<AirportStatDTO> findAirportsByAvgDelay(Instant start, Instant end);

    // AR6 Aeroporti ordinati per numero di aeroporti connessi in uscita
    // NEO4J FATTA SU AirportRepository

    // AR7 Classifica delle Città per traffico aereo totale
    // NEO4J FATTA SU CityRepository


    // AR8 Restituisce tutte le statistiche di una città specifica
    // AR8 parte 1
    @Query(value = "{ 'route.origin.iata': { '$in': ?0 }, 'flight_info.schedule.departure_datetime': { '$gte': ?1, '$lte': ?2 } }", count = true)
    long countDeparturesByAirports(List<String> iataCodes, Instant start, Instant end);

    // AR8 parte 2 = Conta gli arrivi in una lista di IATA nel time range
    @Query(value = "{ 'route.destination.iata': { '$in': ?0 }, 'flight_info.schedule.departure_datetime': { '$gte': ?1, '$lte': ?2 } }", count = true)
    long countArrivalsByAirports(List<String> iataCodes, Instant start, Instant end);


    // AR9 Restituisce i giorni della settimana ordinati per delay medio nel time interval
    @Aggregation(pipeline = {
            "{ '$match': { " +
                    "'flight_info.schedule.departure_datetime': { '$gte': ?0, '$lte': ?1 }, " +
                    "'stats.tot_delay_minutes': { '$ne': null } " +
                    "} }",
            "{ '$project': { " +
                    "'dayOfWeek': { '$dayOfWeek': '$flight_info.schedule.departure_datetime' }, " +
                    "'delay': '$stats.tot_delay_minutes' " +
                    "} }",
            "{ '$group': { " +
                    "'_id': '$dayOfWeek', " +
                    "'avgDelay': { '$avg': '$delay' } " +
                    "} }",
            "{ '$sort': { 'avgDelay': -1 } }",
            "{ '$project': { " +
                    "'_id': 0, " +
                    "'dayOfWeek': '$_id', " +
                    "'avgDelay': 1 " +
                    "} }"
    })
    List<DayStatsDTO> findDaysByAvgDelay(Instant start, Instant end);


    // AR10 Restituisce data un Airline e un time intervall: i total km flown, l'avg delay, l'avg KM percorsi, l'ariline efficiency e il # of flights
    @Aggregation(pipeline = {
            "{ '$match': { " +
                    "'flight_info.schedule.departure_datetime': { '$gte': ?0, '$lte': ?1 }, " +
                    "'flight_info.airline.iata': ?2 " +
                    "} }",
            "{ '$group': { " +
                    "'_id': '$flight_info.airline.iata', " +
                    "'totalKm': { '$sum': '$route.distance_km' }, " +
                    "'avgKm': { '$avg': '$route.distance_km' }, " +
                    "'totalFlights': { '$sum': 1 }, " +
                    "'avgDelay': { '$avg': '$stats.tot_delay_minutes' }, " +
                    "'totalDuration': { '$sum': '$stats.air_time_minutes' }" +
                    "} }",
            "{ '$project': { " +
                    "'_id': 0, " +
                    "'airlineIata': '$_id', " +
                    "'totalKm': 1, " +
                    "'avgKm': 1, " +
                    "'avgDelay': 1, " +
                    "'totalFlights': 1, " +
                    //calcolo efficiency
                    "'efficiency': { " +
                    "'$cond': [ " +
                    "{ '$eq': ['$totalDuration', 0] }, " + // Se la durata è 0, evita divisione per zero
                    "0, " +
                    "{ '$divide': ['$totalKm', '$totalDuration'] }" +
                    "] " +
                    "}" +
                    "} }"
    })
    List<AirlineReportDTO> generateAirlineReport(Instant start, Instant end, String AirlineIata);


    @Query("{ 'flight_info.flight_key': ?0 }")
    @Update("{ '$set': { 'flight_log': ?1 } }")
    void updateFlightLogByKey(String flightKey, FlightMongo.FlightLog log);

}



