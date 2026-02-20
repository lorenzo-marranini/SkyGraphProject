package it.unipi.SkyGraph.repository;

import it.unipi.SkyGraph.dto.*;
import it.unipi.SkyGraph.model.Airport;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AirportRepository extends Neo4jRepository<Airport, String> {


    //CRUD flights
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

    // TC7
    @Aggregation(pipeline = {
            """
            {
                $geoNear: {
                    near: { type: 'Point', coordinates: [ ?0, ?1 ] },
                    distanceField: 'distanceKm',
                    spherical: true,
                    distanceMultiplier: 0.001
                }
            }
            """,
            "{ $limit: 10 }"
    })
    List<AirportEmergencyDTO> findNearestAirports(double longitude, double latitude);


    // TC8 Dato un aeroporto chiuso, trovare un altro aeroporto che abbia il piu alto rapporto tra connessioni in comune fratto distanza
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
            "WHERE distKm > 0 " +
            // Calcola il rapporto e mappa i campi all'interfaccia AirportRankingDTO
            "RETURN alt.iata_code AS iataCode, alt.name AS name, (sharedConnections / distKm) AS score " +
            "ORDER BY score DESC " +
            "LIMIT 5")
    List<AirportRankingDTO> findBestAlternativeAirports(@Param("closedIata") String closedIata);

    // ----------------------- AIRLINE REPRESENTATIVE ------------------------------------

    // AR1  Cercare un volo possibile dato origin e destinatio e numero di scali
    // AR3 Rotta più veloce (Weighted Shortest Path basato su mean_scheduled_time)
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

    // AR1 PARTE 2
    @Query("MATCH p = (start:Airport {iata_code: $origin})-[:ROUTE*1..4]->(end:Airport {iata_code: $dest}) " +
            "WHERE length(p) <= $maxHops " +
            "WITH [n in nodes(p) | n.iata_code] AS codes " +
            "RETURN reduce(s = head(codes), x in tail(codes) | s + ',' + x) " +
            "LIMIT 10")
    List<String> findCandidatePaths(@Param("origin") String origin,
                                    @Param("dest") String dest,
                                    @Param("maxHops") int maxHops);


    // AR4 Visualizzare gli aeroporti ordinati per il betweenness centrality score
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
            "RETURN airport.iata_code AS iataCode, airport.name AS name, score AS score " +
            "ORDER BY score DESC LIMIT 30")
    List<AirportRankingDTO> findTopHubsByRank();


    // AR6 Aeroporti ordinati per numero di aeroporti connessi in uscita
    @Query("MATCH (a:Airport)-[r:ROUTE]->() " +
            "RETURN a.iata_code AS iataCode, a.name AS name, count(r) AS score " +
            "ORDER BY score DESC " +
            "LIMIT 20")
    List<AirportRankingDTO> findAirportsConnections();



}