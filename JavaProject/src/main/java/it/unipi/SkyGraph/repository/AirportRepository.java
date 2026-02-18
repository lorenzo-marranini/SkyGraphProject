package it.unipi.SkyGraph.repository;

import it.unipi.SkyGraph.dto.AirportDTO;
import it.unipi.SkyGraph.dto.AirportRankingDTO;
import it.unipi.SkyGraph.dto.RouteStatsDTO;
import it.unipi.SkyGraph.model.Airport;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface AirportRepository extends Neo4jRepository<Airport, String> {

    // Ricerca base per IATA
    Optional<Airport> findByIataCode(String iataCode);

    // ---------- GUEST-----------------





    // ------------------------- Traffic Controller ---------------------

    // 5) Aeroporti ordinati per numero di rotte , senza intervallo di tempo
    // 5) con intervallo di tempo fatta su mongo in FlighRepository

    @Query("MATCH (a:Airport)-[r:ROUTE]->() " +
            "RETURN a.iata_code AS iataCode, a.name AS name, count(r) AS score " +
            "ORDER BY score DESC " +
            "LIMIT 20")
    List<AirportRankingDTO> findAirportsConnections();

    // 2. Aeroporti ordinati per numero di voli totali
    @Query("MATCH (a:Airport)-[r:ROUTE]->() " +
            "RETURN a.iata_code AS iataCode, a.name AS name, sum(r.num_flights) AS score " +
            "ORDER BY score DESC " +
            "LIMIT 20")
    List<AirportRankingDTO> findAirportsByTotalFlights();

    // 3. Rotte ordinate per Durata Media
    @Query("MATCH (start:Airport)-[r:ROUTE]->(end:Airport) " +
            "RETURN start.name AS origin, end.name AS destination, r.mean_scheduled_time AS score " +
            "ORDER BY score DESC " +
            "LIMIT 20")
    List<RouteStatsDTO> findLongestRoutes();

    // 4. Rotte ordinate per Volume di Traffico
    @Query("MATCH (start:Airport)-[r:ROUTE]->(end:Airport) " +
            "RETURN start.name AS origin, end.name AS destination, r.num_flights AS score " +
            "ORDER BY score DESC " +
            "LIMIT 20")
    List<RouteStatsDTO> findBusiestRoutes();



    // --- QUERY PER IL TRAFFIC CONTROLLER ---



    // --- QUERY PER L'AIRLINE REPRESENTATIVE ---


    // 1. Trova il percorso più breve (meno scali) tra due aeroporti [DA SISTEMARE CON LA PARTE IN MONGO]
    @Query("MATCH (start:Airport {iata_code: $origin}), (end:Airport {iata_code: $dest}) " +
            "MATCH p = shortestPath((start)-[:ROUTE*..5]->(end)) " +
            "RETURN p")
    List<AirportDTO> findShortestPath(@Param("origin") String origin, @Param("dest") String dest);



    // 2. Visualizzare gli aeroporti ordinati per il network centrality score
    @Query("CALL gds.pageRank.stream({ " +
            "  nodeProjection: 'Airport', " +
            "  relationshipProjection: { " +
            "    ROUTE: { " +
            "      type: 'ROUTE', " +
            "      properties: 'num_flights' " +
            "    } " +
            "  }, " +
            "  relationshipWeightProperty: 'num_flights' " +
            "}) " +
            "YIELD nodeId, score " +
            "WITH gds.util.asNode(nodeId) AS airport, score " +
            "RETURN airport.iata_code AS iataCode, airport.name AS name, score AS networkScore " +
            "ORDER BY score DESC LIMIT 30")
    List<AirportRankingDTO> findTopHubsByPageRank();
}