package it.unipi.SkyGraph.repository;

import it.unipi.SkyGraph.dto.AirportDTO;
import it.unipi.SkyGraph.dto.CityStatsDTO;
import it.unipi.SkyGraph.model.City;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CityRepository extends Neo4jRepository<City, String> {

    // 1. Classifica delle Città per traffico aereo totale
    @Query("MATCH (c:City)<-[:LOCATED_IN]-(a:Airport)-[r:ROUTE]->() " +
            "RETURN c.name AS cityName, c.country AS country, " +
            "sum(r.num_flights) AS totalFlights, count(DISTINCT a) AS airportCount " +
            "ORDER BY totalFlights DESC " +
            "LIMIT 20")
    List<CityStatsDTO> findMostTraffickedCities();

    Optional<City> findByNameIgnoreCase(String name);


    @Query("MATCH (c:City)<-[:LOCATED_IN]-(a:Airport) " +
            "WHERE toLower(c.name) = toLower($cityName) " +
            "RETURN a.iata_code")
    List<String> findIataCodesByCity(@Param("cityName") String cityName);

    // 4. Trova la nazione di una città (Case Insensitive)
    @Query("MATCH (c:City) " +
            "WHERE toLower(c.name) = toLower($cityName) " +
            "RETURN c.country LIMIT 1")
    String findCountryByCity(@Param("cityName") String cityName);
}