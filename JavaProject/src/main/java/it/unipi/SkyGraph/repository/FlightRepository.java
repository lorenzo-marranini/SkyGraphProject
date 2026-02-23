package it.unipi.SkyGraph.repository;

import it.unipi.SkyGraph.dto.*;
import it.unipi.SkyGraph.model.FlightMongo;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlightRepository extends MongoRepository<FlightMongo, String>, FlightRepositoryCustom {
    //----------------------------- GUEST--------------------------------------
    // G1 Finds flights by origin, destination and date range (start of day -> end of day)
    @Query("{ 'route.origin.iata': ?0, 'route.destination.iata': ?1, 'flight_info.schedule.departure_datetime': { $gte: ?2, $lt: ?3 } }")
    List<FlightMongo> searchFlights(String origin, String destination, Instant startOfDay, Instant endOfDay);

    // G2 Finds flights in a date range (start -> end) that have a non-null flight_log field, indicating they are live.
    @Query("{ 'flight_info.schedule.departure_datetime': { $gte: ?0, $lt: ?1 }, 'flight_log': { $ne: null } }")
    Page<FlightMongo> searchPagedFlightsLive(Instant start, Instant end, Pageable pageable);

    @Query("{ 'flight_info.schedule.departure_datetime': { $gte: ?0, $lt: ?1 }, 'flight_log': { $ne: null } }")
    List<FlightMongo> getAllFlightsLive(Instant start, Instant end);


    // -------------------------------- TRAFFIC CONTROLLER ------------------------------
    // TC1 Returns airlines ranked by average delay in a given time interval, only considering flights with non-null delay stats
    @Aggregation(pipeline = {
            "{ '$match': { " +
                    "'flight_info.schedule.departure_datetime': { '$gte': ?0, '$lte': ?1 }, " +
                    "'stats.tot_delay_minutes': { '$ne': null } " +
                    "} }",
            "{ '$group': { '_id': '$flight_info.airline.name', 'score': { '$avg': '$stats.tot_delay_minutes' } } }",
            "{ '$sort': { 'score': 1 } }",
            "{ '$limit': ?2 }",
            "{ '$project': { '_id': 0, 'airlineName': '$_id', 'score': 1 } }"
    })
    List<AirlineStatDTO> findAirlinesByAvgDelay(Instant start, Instant end, Integer limit);

    // TC2 Returns airlines ranked by total number of flights in a given time interval
    @Aggregation(pipeline = {
            "{ '$match': { 'flight_info.schedule.departure_datetime': { $gte: ?0, $lt: ?1 } } }",
            "{ '$group': { '_id': '$flight_info.airline.name', 'score': { '$sum': 1 } } }",
            "{ '$sort': { 'score': -1 } }",
            "{ '$limit': ?2 }",
            "{ '$project': { '_id': 0, 'airlineName': '$_id', 'score': 1 } }"
    })
    List<AirlineStatDTO> findAirlinesByTotalFlights(Instant start, Instant end, Integer limit);

    // TC3 Returns airlines ranked by total distance flown in a given time interval, summing the distance of all routes flown by each airline
    @Aggregation(pipeline = {
            "{ '$match': { 'flight_info.schedule.departure_datetime': { '$gte': ?0, '$lte': ?1 } } }",
            "{ '$group': { '_id': '$flight_info.airline.name', 'score': { '$sum': '$route.distance_km' } } }",
            "{ '$sort': { 'score': -1 } }",
            "{ '$project': { '_id': 0, 'airlineName': '$_id', 'score': 1 } }"
    })
    List<AirlineStatDTO> findAirlinesByTotalDistance(Instant start, Instant end);

    // TC4 Returns airlines ranked by average route distance in a given time interval, averaging the distance of all routes flown by each airline
    @Aggregation(pipeline = {
            "{ '$match': { 'flight_info.schedule.departure_datetime': { '$gte': ?0, '$lte': ?1 } } }",
            "{ '$group': { '_id': '$flight_info.airline.name', 'score': { '$avg': '$route.distance_km' } } }",
            "{ '$sort': { 'score': -1 } }",
            "{ '$limit': ?2 }",
            "{ '$project': { '_id': 0, 'airlineName': '$_id', 'score': 1 } }"
    })
    List<AirlineStatDTO> findAirlinesByAvgRouteDistance(Instant start, Instant end, Integer limit);

    // TC5 Returns airlines ranked by number of flights on a specific route (origin -> destination) in a given time interval
    @Aggregation(pipeline = {
            "{ '$match': { 'route.origin.iata': ?0, 'route.destination.iata': ?1, 'flight_info.schedule.departure_datetime': { '$gte': ?2, '$lte': ?3 } } }",
            "{ '$group': { '_id': '$flight_info.airline.name', 'score': { '$sum': 1 } } }",
            "{ '$sort': { 'score': -1 } }",
                "{ '$limit': ?4 }",
            "{ '$project': { '_id': 0, 'airlineName': '$_id', 'score': 1 } }"
    })
    List<AirlineStatDTO> findAirlinesByRoute(String origin, String destination, Instant start, Instant end, Integer limit);

    // TC6 Returns airlines ranked by average delay on a specific route (origin -> destination) in a given time interval, only considering flights with non-null delay stats
    @Aggregation(pipeline = {
            "{ '$match': { 'route.origin.iata': ?0, 'route.destination.iata': ?1, 'flight_info.schedule.departure_datetime': { '$gte': ?2, '$lte': ?3 }, 'stats.tot_delay_minutes': { '$ne': null } } }",
            "{ '$group': { '_id': '$flight_info.airline.name', 'score': { '$avg': '$stats.tot_delay_minutes' } } }",
            "{ '$sort': { 'score': 1 } }",
            "{ '$limit': ?4 }",
            "{ '$project': { '_id': 0, 'airlineName': '$_id', 'score': 1 } }"
    })
    List<AirlineStatDTO> findAirlinesByRouteDelay(String originIata, String destIata, Instant start, Instant end, Integer limit);

    @Query("{ 'flight_info.flight_key': ?0, 'flight_log': { $ne: null } }")
    Optional<FlightMongo> findLiveFlightByKey(String flightKey);

    //------------------------AIRLINE REPRESENTATIVE-----------------------------
    // AR1 Returns the next flights departing from an origin to a destination after a given time, sorted by departure time
    @Query(value = "{ 'route.origin.iata': ?0, 'route.destination.iata': ?1, 'flight_info.schedule.departure_datetime': { $gt: ?2 } }",
            sort = "{ 'flight_info.schedule.departure_datetime': 1 }")
    List<FlightMongo> findNextFlight(String origin, String dest, Instant minTime, Pageable pageable);


    // AR2 Retruns the routes (origin -> destination) ranked by number of flights in a given time interval
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
            "{ '$limit': ?2 }",
            "{ '$project': { " +
                    "'_id': 0, " +
                    "'Origin': '$_id.origin', " +
                    "'Destination': '$_id.destination', " +
                    "'Score': '$count' " +
                    "} }"
    })
    List<RouteStatsDTO> findRoutesByFlightCount(Instant start, Instant end, Integer limit);

    // AR2.1 Returns the routes (origin -> destination) ranked by number of cancelled flights in a given time interval
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
            "{ '$limit': ?2 }",
            "{ '$project': { " +
                    "'_id': 0, " +
                    "'Origin': '$_id.origin', " +
                    "'Destination': '$_id.destination', " +
                    "'Score': '$count' " +
                    "} }"
    })
    List<RouteStatsDTO> findRoutesByCancelledCount(Instant start, Instant end, Integer limit);


    // AR2.2 Returns the routes (origin -> destination) ranked by number of diverted flights in a given time interval
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
            "{ '$limit': ?2 }",
            "{ '$project': { " +
                    "'_id': 0, " +
                    "'Origin': '$_id.origin', " +
                    "'Destination': '$_id.destination', " +
                    "'Score': '$count' " +
                    "} }"
    })
    List<RouteStatsDTO> findRoutesByDivertedCount(Instant start, Instant end, Integer limit);

    // AR5 Returns airports ranked by average delay of departing flights in a given time interval
    @Aggregation(pipeline = {
            "{ '$match': { " +
                    "'flight_info.schedule.departure_datetime': { '$gte': ?0, '$lte': ?1 }, " +
                    "'stats': { '$ne': null } " +
                    "} }",
            "{ '$group': { " +
                    "'_id': '$route.origin.airport_name', " +
                    "'iata': { '$first': '$route.origin.iata' }, " +
                    "'city': { '$first': '$route.origin.city' }, " +
                    "'score': { '$avg': '$stats.tot_delay_minutes' } " +
                    "} }",
            "{ '$sort': { 'score': -1 } }",
            "{ '$limit': ?2 } ",
            "{ '$project': { " +
                    "'_id': 0, " +
                    "'name': '$_id', " +      // Spostiamo l'id del gruppo nel campo 'name'
                    "'iata': 1, " +
                    "'score': 1 " +
                    "} }"
    })
    List<AirportRankingDTO> findAirportsByAvgDelay(Instant start, Instant end, Integer limit);

    // AR8 Returns the number of departures from a list of IATA codes in a given time interval
    // AR8 parte 1
    @Query(value = "{ 'route.origin.iata': { '$in': ?0 }, 'flight_info.schedule.departure_datetime': { '$gte': ?1, '$lte': ?2 } }", count = true)
    long countDeparturesByAirports(List<String> iataCodes, Instant start, Instant end);

    // AR8 parte 2 = Counts arrivals to a list of IATA codes in a given time interval
    @Query(value = "{ 'route.destination.iata': { '$in': ?0 }, 'flight_info.schedule.departure_datetime': { '$gte': ?1, '$lte': ?2 } }", count = true)
    long countArrivalsByAirports(List<String> iataCodes, Instant start, Instant end);

    // AR9 Returns the average delay by day of the week (1=Sunday, 2=Monday, ..., 7=Saturday) for flights in a given time interval
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


    // AR10 Returns a report for a specific airline in a given time interval, including total and average distance flown, total flights, average delay and an efficiency score calculated as total distance divided by total air time (km/min)
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


    // Updates the total delay minutes for a flight and sets the flight_log to null to indicate the flight has been finalized and is no longer live. This is used in the scheduled task that finalizes flights after their scheduled departure time has passed.
    @Query("{ 'flight_info.flight_key': ?0 }")
    @Update("{ '$set': { 'stats.tot_delay_minutes': ?1, 'flight_log': null } }")
    void finalizeFlight(String flightKey, long finalDelay);

    @Query("{ 'flight_info.flight_key': ?0 }")
    Optional<FlightMongo> findByFlightKey(String flightKey);
}



