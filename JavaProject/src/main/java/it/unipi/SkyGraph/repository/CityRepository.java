package it.unipi.SkyGraph.repository;

import it.unipi.SkyGraph.dto.AirportDTO;
import it.unipi.SkyGraph.dto.CityStatsDTO;
import it.unipi.SkyGraph.model.City;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CityRepository extends Neo4jRepository<City, String> {

    // 1. Classifica delle Città per traffico aereo totale
    @Query("MATCH (c:City)<-[:LOCATED_IN]-(a:Airport)-[r:ROUTE]->() " +
            "RETURN c.name AS cityName, c.country AS country, " +
            "sum(r.num_flights) AS totalFlights, count(DISTINCT a) AS airportCount " +
            "ORDER BY totalFlights DESC " +
            "LIMIT 20")
    List<CityStatsDTO> findMostTraffickedCities();

    // 2. Cerca tutti gli aeroporti di una specifica città
    @Query("MATCH (c:City {name: $cityName})<-[:LOCATED_IN]-(a:Airport) " +
            "RETURN a.iata_code AS iataCode, a.name AS name, a.latitude AS latitude, a.longitude AS longitude")
    List<AirportDTO> findAirportsInCity(@Param("cityName") String cityName);

}