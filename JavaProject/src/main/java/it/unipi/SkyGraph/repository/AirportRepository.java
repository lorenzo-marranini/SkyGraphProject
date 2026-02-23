package it.unipi.SkyGraph.repository;

import ch.qos.logback.core.read.ListAppender;
import it.unipi.SkyGraph.dto.*;
import it.unipi.SkyGraph.model.Airport;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface AirportRepository extends Neo4jRepository<Airport, String> {

    /**
     * Upserts a ROUTE relationship between two airports. On creation, sets {@code num_voli = 1}
     * and {@code mean_scheduled_time} to the given duration. On match, incrementally updates
     * the running average using the formula: (old_mean * n + new_value) / (n + 1).
     */
    @Query("MATCH (a:Airport {iata_code: $originIata}), (b:Airport {iata_code: $destIata}) " +
            "MERGE (a)-[r:ROUTE]->(b) " +
            "ON CREATE SET r.num_voli = 1, r.mean_scheduled_time = toFloat($duration) " +
            "ON MATCH SET r.mean_scheduled_time = ((r.mean_scheduled_time * r.num_voli) + toFloat($duration)) / (r.num_voli + 1), " +
            "             r.num_voli = r.num_voli + 1")
    void upsertRouteRelationship(@Param("originIata") String originIata,
                                 @Param("destIata") String destIata,
                                 @Param("duration") double duration);
    // --------------------------- GUEST-------------------------

    Optional<Airport> findByIataCode(String iataCode);


    // ------------------------------- TRAFFIC CONTROLLER ------------------------
    /**
     * Finds the best alternative airports for a closed one. Matches destinations reachable from
     * the closed airport, then finds other airports sharing those destinations. Filters candidates
     * within 200 km and ranks them by {@code sharedConnections / log(distKm + 1)} to balance
     * connectivity and proximity.
     */
    @Query("MATCH (closed:Airport {iata_code: $closedIata})-[:ROUTE]->(dest:Airport) " +
            "MATCH (alt:Airport)-[:ROUTE]->(dest) " +
            "WHERE alt.iata_code <> $closedIata " +
            "WITH closed, alt, count(DISTINCT dest) AS sharedConnections " +
            // Calcola la distanza geospaziale tra i due aeroporti (convertita in km)
            "WITH alt, sharedConnections, " +
            "     point.distance( " +
            "       point({latitude: closed.latitude, longitude: closed.longitude}), " +
            "       point({latitude: alt.latitude, longitude: alt.longitude}) " +
            "     ) / 1000.0 AS distKm " +
            "WHERE distKm > 0 AND distKm < 200 " +
            // Calcola il rapporto e mappa i campi all'interfaccia AirportRankingDTO
            "RETURN alt.iata_code AS iata, alt.name AS name, (sharedConnections / log(distKm + 1)) AS score, '' as scoreType " +
            "ORDER BY score DESC " +
            "LIMIT 5")
    List<AirportRankingDTO> findBestAlternativeAirports(@Param("closedIata") String closedIata);

    // ----------------------- AIRLINE REPRESENTATIVE ------------------------------------
    /**
     * Returns the quickest path (up to {@code maxHops} hops) by summing {@code mean_scheduled_time}
     * across ROUTE relationships via {@code reduce}. Returns total duration and the ordered list
     * of airports in the path.
     */
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

    /**
     * Returns up to 10 candidate IATA code sequences as comma-separated strings (e.g. "JFK,LHR,FCO")
     * for paths with at most {@code maxHops} hops. Used to enumerate possible routes before
     * applying real-flight filtering.
     */
    @Query("MATCH p = (start:Airport {iata_code: $origin})-[:ROUTE*1..4]->(end:Airport {iata_code: $dest}) " +
            "WHERE length(p) <= $maxHops " +
            "WITH [n in nodes(p) | n.iata_code] AS codes " +
            "RETURN reduce(s = head(codes), x in tail(codes) | s + ',' + x) " +
            "LIMIT 10")
    List<String> findCandidatePaths(@Param("origin") String origin,
                                    @Param("dest") String dest,
                                    @Param("maxHops") int maxHops);


    /**
     * Ranks airports by a weighted hub score: {@code directTraffic * 0.6 + indirectTraffic * 0.4},
     * where {@code directTraffic} is the total outgoing flights and {@code indirectTraffic} is the
     * sum of outgoing flights from all directly reachable airports (two-hop influence).
     */
    @Query("MATCH (airport:Airport)-[r1:ROUTE]->(dest:Airport) " +
            // 1. Calcoliamo il traffico diretto in uscita
            "WITH airport, sum(r1.num_voli) AS directTraffic " +

            // 2. Facciamo un "salto" in avanti: guardiamo quanto sono connessi gli aeroporti di destinazione
            "MATCH (airport)-[:ROUTE]->(dest:Airport)-[r2:ROUTE]->() " +
            "WITH airport, directTraffic, sum(r2.num_voli) AS indirectTraffic " +

            // 3. Calcoliamo lo score pesato e mappiamo i campi per il DTO
            "RETURN airport.iata_code AS iata, " + // Ricorda di verificare se qui ci va iata o iata_code!
            "       airport.name AS name, " +
            "       (directTraffic * 0.6 + indirectTraffic * 0.4) AS score, '' AS scoreType " +
            "ORDER BY score DESC " +
            "LIMIT $limit")
    List<AirportRankingDTO> findTopHubsByRank(@Param("limit") Integer limit);


    // AR6
    @Query("MATCH (a:Airport)-[r:ROUTE]->() " +
            "RETURN a.iata_code AS iata, a.name AS name, count(r) AS score, '' as scoreType " +
            "ORDER BY score DESC " +
            "LIMIT $limit")
    List<AirportRankingDTO> findAirportsConnections(@Param("limit") Integer limit);

    /**
     * Decrements {@code num_voli} and recalculates {@code mean_scheduled_time} by reversing the
     * incremental average. If {@code num_voli} reaches 0 after decrement, the ROUTE relationship
     * is deleted entirely.
     */
    @Query("MATCH (a:Airport {iata_code: $originIata})-[r:ROUTE]->(b:Airport {iata_code: $destIata}) " +
            "SET r.mean_scheduled_time = CASE " +
            "    WHEN r.num_voli > 1 THEN ((r.mean_scheduled_time * r.num_voli) - toFloat($duration)) / (r.num_voli - 1) " +
            "    ELSE 0 END, " +
            "r.num_voli = r.num_voli - 1 " +
            "WITH r WHERE r.num_voli = 0 " +
            "DELETE r")
    void decrementRouteRelationship(@Param("originIata") String originIata,
                                    @Param("destIata") String destIata,
                                    @Param("duration") double duration);

}