package it.unipi.SkyGraph.repository;

import it.unipi.SkyGraph.dto.*;
import it.unipi.SkyGraph.model.Airport;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AirportRepository extends Neo4jRepository<Airport, String> {

    // Ricerca base per IATA
    Optional<Airport> findByIataCode(String iataCode);

    // --------------------------- GUEST-------------------------


    // ------------------------------- TRAFFIC CONTROLLER ------------------------

    // 4) Aeroporti ordinati per numero di rotte

    @Query("MATCH (a:Airport)-[r:ROUTE]->() " +
            "RETURN a.iata_code AS iataCode, a.name AS name, count(r) AS score " +
            "ORDER BY score DESC " +
            "LIMIT 20")
    List<AirportRankingDTO> findAirportsConnections();

    // 9) Dato un aeroporto chiuso, trovare un altro aeroporto che abbia il piu alto rapporto tra connessioni in comune fratto distanza
    // TO DO: Qeery in Neo4j


    // ----------------------- AIRLINE REPRESENTATIVE ------------------------------------


    // 1) Given origin* destination* stops*(scali) and date* (default=today), find the possible flights with less time flown.
    // parte in neo4j TO DO

    // QUERY: Rotta più veloce (Weighted Shortest Path basato su mean_scheduled_time)
    @Query("MATCH p = (start:Airport {iata_code: $origin})-[:ROUTE*1..3]->(end:Airport {iata_code: $dest}) " +
            "WHERE length(p) <= $maxHops " +
            "WITH p, reduce(weight = 0.0, r in relationships(p) | weight + r.mean_scheduled_time) AS totalTime " +
            "ORDER BY totalTime ASC " +
            "LIMIT 1 " +
            "RETURN totalTime AS totalDuration, " +
            "       [n in nodes(p) | {iataCode: n.iata_code, name: n.name}] AS path")
    Optional<QuickestPathDTO> findQuickestRoute(
            @Param("origin") String origin,
            @Param("dest") String dest,
            @Param("maxHops") int maxHops
    );


    // 2) Visualizzare gli aeroporti ordinati per il betweenness centrality score
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


    @Query("MATCH p = (start:Airport {iata_code: $origin})-[:ROUTE*1..4]->(end:Airport {iata_code: $dest}) " +
            "WHERE length(p) <= $maxHops " +
            "WITH [n in nodes(p) | n.iata_code] AS codes " +
            // Questa funzione 'reduce' unisce la lista in una stringa separata da virgole
            "RETURN reduce(s = head(codes), x in tail(codes) | s + ',' + x) " +
            "LIMIT 10")
    List<String> findCandidatePaths(@Param("origin") String origin,
                                    @Param("dest") String dest,
                                    @Param("maxHops") int maxHops);
}