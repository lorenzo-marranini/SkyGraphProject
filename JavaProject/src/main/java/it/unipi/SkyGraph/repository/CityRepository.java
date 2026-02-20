package it.unipi.SkyGraph.repository;

import it.unipi.SkyGraph.dto.CityRankDTO;
import it.unipi.SkyGraph.model.City;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CityRepository extends Neo4jRepository<City, String> {

    // AR7 Classifica delle Città per traffico aereo totale
    @Query("MATCH (c:City)<-[:LOCATED_IN]-(a:Airport)-[r:ROUTE]->() " +
            "RETURN c.name AS CityName, c.state_id AS StateId, " +
            "sum(r.num_voli) AS TotalFlights, count(DISTINCT a) AS AirportCount " +
            "ORDER BY TotalFlights DESC " +
            "LIMIT 20")
    List<CityRankDTO> findMostTraffickedCities();

    Optional<City> findByNameIgnoreCase(String name);

    // AR8 parte 1
    @Query("MATCH (c:City)<-[:LOCATED_IN]-(a:Airport) " +
            "WHERE toLower(c.name) = toLower($cityName) " +
            "RETURN a.iata_code")
    List<String> findIataCodesByCity(@Param("cityName") String cityName);

    // AR8 parte 2
    @Query("MATCH (c:City) " +
            "WHERE toLower(c.name) = toLower($cityName) " +
            "RETURN c.state_id as StateId LIMIT 1")
    String findCountryByCity(@Param("cityName") String cityName);




}