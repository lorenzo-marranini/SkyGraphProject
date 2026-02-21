package it.unipi.SkyGraph.service;

import it.unipi.SkyGraph.config.SimulationClock;
import it.unipi.SkyGraph.dto.*;
import it.unipi.SkyGraph.enums.TimeInterval;
import it.unipi.SkyGraph.model.Airport;
import it.unipi.SkyGraph.model.AirportMongo;
import it.unipi.SkyGraph.model.City;
import it.unipi.SkyGraph.repository.AirportRepository;
import it.unipi.SkyGraph.repository.CityRepository;
import it.unipi.SkyGraph.repository.FlightRepository;
import it.unipi.SkyGraph.repository.AirportMongoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AirportService {

    private final AirportRepository airportRepository;
    private final AirportMongoRepository airportMongoRepository;
    private final FlightRepository flightRepository;
    private final SimulationClock clock;
    private final CityRepository cityRepository;
    // ----------- GUEST ---------
    public Optional<Airport> getAirportByIata(String iataCode) {
        return airportRepository.findByIataCode(iataCode);
    }

    // --------- TRAFFIC CONTROLLER ---------


    // TC7
    public List<AirportEmergencyDTO> getNearestAirportsForEmergency(String flightKey) {
        return flightRepository.findLiveFlightByKey(flightKey)
                .map(flight -> {
                    List<Double> coords = flight.getFlightLog().getLocation().getCoordinates();

                    return airportMongoRepository.findNearestAirports(coords.get(0), coords.get(1));
                })
                // Se il volo non esiste, restituiamo una lista vuota invece di un Optional
                .orElse(java.util.Collections.emptyList());
    }


    // TC8
    public List<AirportRankingDTO> getBestAlternativeAirports(String closedIata) {
        List<AirportRankingDTO> result = airportRepository.findBestAlternativeAirports(closedIata);
        result.forEach(dto  -> dto.setScoreType("SHARED_CONNECTIONS/KM_DISTANCE"));
        return result;
    }


    // ------------ AIRLINE REPRESENTATIVE ----------

    // AR3
    public Optional<QuickestPathDTO> getQuickestRoute(String origin, String dest, int maxHops) {
        return airportRepository.findQuickestRoute(origin, dest, maxHops);
    }

    // AR4
    public List<AirportRankingDTO> getTopHubsByRank(Integer limit) {
        List<AirportRankingDTO> result = airportRepository.findTopHubsByRank(limit);
        result.forEach(dto  -> dto.setScoreType("BETWEENNESS_SCORE"));
        return result;
    }

    // AR5
    public List<AirportStatDTO> getAirportsByAvgDelay(TimeInterval range, Integer limit) {
        List<AirportStatDTO> result =  flightRepository.findAirportsByAvgDelay(
                clock.calculateMinDate(range),
                clock.getSimulatedNowInstant(),
                limit
        );

        result.forEach(dto  -> dto.setScoreType("AVG_DELAY_MIN"));
        return result;
    }

    // AR6
    public List<AirportRankingDTO> getAirportsConnections(Integer limit) {
        List<AirportRankingDTO> result = airportRepository.findAirportsConnections(limit);
        result.forEach(dto  -> dto.setScoreType("NUMBER_OF_CONNECTIONS"));

        return result;
    }

    @Transactional
    public Airport createAirport(Airport airport, String cityName, String country) {
        // ================= 1. NEO4J =================
        City city = cityRepository.findByNameIgnoreCase(cityName)
                .orElseThrow(() -> new IllegalArgumentException("Cannot create airport: City '" + cityName + "' not found in Neo4j."));

        airport.setCity(city);
        Airport savedNeo4jAirport = airportRepository.save(airport);

        // ================= 2. MONGODB =================
        AirportMongo.Location location = new AirportMongo.Location();
        location.setType("Point");
        location.setCoordinates(List.of(airport.getLongitude(), airport.getLatitude()));

        AirportMongo mongoDoc = new AirportMongo();
        mongoDoc.setId(airport.getIataCode());
        mongoDoc.setName(airport.getName());
        mongoDoc.setCity(city.getName());
        mongoDoc.setState(city.getStateId());
        mongoDoc.setCountry(country);
        mongoDoc.setLocation(location);

        airportMongoRepository.save(mongoDoc);

        return savedNeo4jAirport;
    }

    @Transactional
    public Airport updateAirport(Airport airport) {
        // ================= 1. NEO4J =================
        Airport existing = airportRepository.findById(airport.getIataCode())
                .orElseThrow(() -> new IllegalArgumentException("Airport not found in Neo4j"));

        existing.setName(airport.getName());
        existing.setLatitude(airport.getLatitude());
        existing.setLongitude(airport.getLongitude());

        Airport savedAirport = airportRepository.save(existing);

        // ================= 2. MONGODB =================
        AirportMongo mongoDoc = airportMongoRepository.findById(airport.getIataCode())
                .orElseThrow(() -> new IllegalArgumentException("Airport not found in MongoDB"));

        mongoDoc.setName(airport.getName());
        mongoDoc.getLocation().setCoordinates(List.of(airport.getLongitude(), airport.getLatitude()));

        airportMongoRepository.save(mongoDoc);

        return savedAirport;
    }

    @Transactional
    public void deleteAirport(String iataCode) {
        // ================= 1. NEO4J =================
        airportRepository.deleteById(iataCode);

        // ================= 2. MONGODB =================
        airportMongoRepository.deleteById(iataCode);
    }
    public List<AirportMongo> getAllAirports() {
        return airportMongoRepository.findAll();
    }

    public AirportMongo getExistingAirportByIata(String iataCode) {
        return airportMongoRepository.findById(iataCode)
                .orElseThrow(() -> new IllegalArgumentException("Airport not found"));
    }



}