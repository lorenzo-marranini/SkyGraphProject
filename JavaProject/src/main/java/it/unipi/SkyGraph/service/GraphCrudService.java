package it.unipi.SkyGraph.service;

import it.unipi.SkyGraph.model.Airport;
import it.unipi.SkyGraph.model.AirportMongo;
import it.unipi.SkyGraph.model.City;
import it.unipi.SkyGraph.repository.AirportRepository;
import it.unipi.SkyGraph.repository.CityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GraphCrudService {

    private final CityRepository cityRepository;
    private final AirportRepository airportRepository;
    private final MongoTemplate mongoTemplate;
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
    public Airport createAirport(Airport airport, String cityName, String country) {
        City city = cityRepository.findByNameIgnoreCase(cityName)
                .orElseThrow(() -> new IllegalArgumentException("Cannot create airport: City '" + cityName + "' not found in Neo4j."));

        airport.setCity(city);
        Airport savedNeo4jAirport = airportRepository.save(airport);

        // ================= 2. MONGODB =================
        // Construct the inner Location object (GeoJSON format requires Longitude, then Latitude)
        AirportMongo.Location location = new AirportMongo.Location();
        location.setType("Point");
        location.setCoordinates(List.of(airport.getLongitude(), airport.getLatitude()));

        // Construct the Mongo document using your existing model
        AirportMongo mongoDoc = new AirportMongo();
        mongoDoc.setId(airport.getIataCode());
        mongoDoc.setName(airport.getName());
        mongoDoc.setCity(city.getName());
        mongoDoc.setState(city.getStateId());
        mongoDoc.setCountry(country);
        mongoDoc.setLocation(location);

        // Save directly to MongoDB. Since your model is annotated with @Document(collection = "airport"),
        // MongoTemplate will automatically route it to the correct collection.
        mongoTemplate.save(mongoDoc);

        return savedNeo4jAirport;
    }

    public Airport updateAirport(Airport airport) {
        // Find existing to preserve the city relationship if not provided in the update
        Airport existing = airportRepository.findById(airport.getIataCode())
                .orElseThrow(() -> new IllegalArgumentException("Airport not found"));

        existing.setName(airport.getName());
        existing.setLatitude(airport.getLatitude());
        existing.setLongitude(airport.getLongitude());

        return airportRepository.save(existing);
    }

    public List<Airport> getAllAirports() {
        return airportRepository.findAll();
    }

    public Airport getAirportByIata(String iataCode) {
        return airportRepository.findById(iataCode)
                .orElseThrow(() -> new IllegalArgumentException("Airport not found"));
    }

    public void deleteAirport(String iataCode) {
        airportRepository.deleteById(iataCode);
    }
}