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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    private AirportRankingDTO convertToRankingDTO(Airport airport, Double score, String scoreType) {
        if (airport == null) return null;

        AirportRankingDTO dto = new AirportRankingDTO();
        dto.setIata(airport.getIataCode());
        dto.setName(airport.getName());
        dto.setScore(score);
        dto.setScoreType(scoreType);

        return dto;
    }
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

        result.forEach(dto  -> dto.setScoreType("SIMILARITY_SCORE AS SHARED_CONNECTIONS_LOG_DISTANCE"));

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
    public List<AirportRankingDTO> getAirportsByAvgDelay(TimeInterval range, Integer limit) {
        List<AirportRankingDTO> result =  flightRepository.findAirportsByAvgDelay(
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

    // CRUD QUERIES
    @Transactional
    public Airport createAirport(AirportUpdateDTO dto) {
        // ================= 1. NEO4J =================
        // get the city from the CityName
        City city = cityRepository.findByNameIgnoreCase(dto.getCityName())
                .orElseThrow(() -> new IllegalArgumentException("Cannot create airport: City '" + dto.getCityName() + "' not found in Neo4j."));


        Airport airport = new Airport();
        airport.setIataCode(dto.getIataCode());
        airport.setName(dto.getName());
        airport.setLatitude(dto.getLatitude());
        airport.setLongitude(dto.getLongitude());
        airport.setCity(city);

        Airport savedNeo4jAirport = airportRepository.save(airport);

        // ================= 2. MONGODB =================
        AirportMongo.Location location = new AirportMongo.Location();
        location.setType("Point");
        location.setCoordinates(List.of(dto.getLongitude(), dto.getLatitude()));

        AirportMongo mongoDoc = new AirportMongo();
        mongoDoc.setId(dto.getIataCode());
        mongoDoc.setName(dto.getName());
        mongoDoc.setCity(city.getName());
        mongoDoc.setState(city.getStateId());
        mongoDoc.setCountry(dto.getCountry());
        mongoDoc.setLocation(location);

        airportMongoRepository.save(mongoDoc);

        return savedNeo4jAirport;
    }


    @Transactional
    public Airport updateAirport(AirportUpdateDTO dto) {
        // ================= 1. NEO4J =================
        String iataCode = dto.getIataCode();
        Airport existingNeo = airportRepository.findById(iataCode)
                .orElseThrow(() -> new IllegalArgumentException("Airport not found in Neo4j"));

        existingNeo.setName(dto.getName());
        existingNeo.setLatitude(dto.getLatitude());
        existingNeo.setLongitude(dto.getLongitude());

        //if the user changed city, update it making sure the city exists
        if (!existingNeo.getCity().getName().equalsIgnoreCase(dto.getCityName())) {
            City newCity = cityRepository.findByNameIgnoreCase(dto.getCityName())
                    .orElseThrow(() -> new IllegalArgumentException("Cannot update: City '" + dto.getCityName() + "' not found in Neo4j."));
            existingNeo.setCity(newCity);
        }

        Airport savedNeo4jAirport = airportRepository.save(existingNeo);

        // ================= 2. MONGODB =================
        AirportMongo existingMongo = airportMongoRepository.findById(iataCode)
                .orElseThrow(() -> new IllegalArgumentException("Airport not found in MongoDB"));

        existingMongo.setName(dto.getName());
        existingMongo.setCity(dto.getCityName());
        existingMongo.setCountry(dto.getCountry());

        existingMongo.setState(existingNeo.getCity().getStateId());

        existingMongo.getLocation().setCoordinates(List.of(dto.getLongitude(), dto.getLatitude()));

        airportMongoRepository.save(existingMongo);

        return savedNeo4jAirport;
    }

    @Transactional
    public void deleteAirport(String iataCode) {
        // ================= 1. NEO4J =================
        airportRepository.deleteById(iataCode);

        // ================= 2. MONGODB =================
        airportMongoRepository.deleteById(iataCode);
    }
    public Page<AirportMongo> getAllAirports(Pageable pageble) {
        return airportMongoRepository.findAll(pageble);
    }

    public AirportMongo getExistingAirportByIata(String iataCode) {
        return airportMongoRepository.findById(iataCode)
                .orElseThrow(() -> new IllegalArgumentException("Airport not found"));
    }



}