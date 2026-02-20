package it.unipi.SkyGraph.service;

import it.unipi.SkyGraph.model.Airport;
import it.unipi.SkyGraph.model.AirportMongo;
import it.unipi.SkyGraph.model.City;
import it.unipi.SkyGraph.repository.AirportMongoRepository;
import it.unipi.SkyGraph.repository.AirportRepository;
import it.unipi.SkyGraph.repository.CityRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GraphCrudService {

    private final CityRepository cityRepository;
    private final AirportRepository airportRepository;
    private final AirportMongoRepository airportMongoRepository;
    // --- CITY CRUD ---
    public City createOrUpdateCity(City city) {
        return cityRepository.save(city);
    }

    public List<City> getAllCities() {
        return cityRepository.findAll();
    }

    public City getCityById(String cityState) {
        return cityRepository.findById(cityState)
                .orElseThrow(() -> new IllegalArgumentException("City not found"));
    }

    public void deleteCity(String cityState) {
        cityRepository.deleteById(cityState);
    }

    // --- AIRPORT CRUD ---
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

    public AirportMongo getAirportByIata(String iataCode) {
        return airportMongoRepository.findById(iataCode)
                .orElseThrow(() -> new IllegalArgumentException("Airport not found"));
    }

}