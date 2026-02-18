package it.unipi.SkyGraph.controller;

import it.unipi.SkyGraph.dto.AirportDTO;
import it.unipi.SkyGraph.dto.AirportRankingDTO;
import it.unipi.SkyGraph.dto.RouteStatsDTO;
import it.unipi.SkyGraph.model.Airport;
import it.unipi.SkyGraph.repository.AirportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/neo4j")
@RequiredArgsConstructor
public class AirportController {

    private final AirportRepository airportRepository;

    // --- ENDPOINT PUBBLICI (Guest) ---

    // 1. Dettagli aeroporto per IATA
    @GetMapping("/airport/{iata}")
    public ResponseEntity<Airport> getAirportByIata(@PathVariable String iata) {
        Optional<Airport> airport = airportRepository.findByIataCode(iata);
        return airport.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 2. Classifica aeroporti per numero di rotte
//    @GetMapping("/rankings/connections")
//    public ResponseEntity<List<AirportRankingDTO>> getAirportsByRoutesCount() {
//        return ResponseEntity.ok(airportRepository.findAirportsByRoutesCount());
//    }

    // 3. Classifica aeroporti per volume voli totali
    @GetMapping("/rankings/volume")
    public ResponseEntity<List<AirportRankingDTO>> getAirportsByTotalFlights() {
        return ResponseEntity.ok(airportRepository.findAirportsByTotalFlights());
    }

    // 4. Classifica rotte più lunghe (Durata media)
    @GetMapping("/routes/longest")
    public ResponseEntity<List<RouteStatsDTO>> getLongestRoutes() {
        return ResponseEntity.ok(airportRepository.findLongestRoutes());
    }

    // 5. Classifica rotte più trafficate
    @GetMapping("/routes/busiest")
    public ResponseEntity<List<RouteStatsDTO>> getBusiestRoutes() {
        return ResponseEntity.ok(airportRepository.findBusiestRoutes());
    }


    // --- ENDPOINT PROTETTI (Airline Representative) ---

    // 6. Percorso più breve (Shortest Path)
    @PreAuthorize("hasAnyRole('AIRLINE_REPRESENTATIVE', 'ADMIN')")
    @GetMapping("/shortest-path")
    public ResponseEntity<List<AirportDTO>> getShortestPath(
            @RequestParam String origin,
            @RequestParam String dest) {

        List<AirportDTO> path = airportRepository.findShortestPath(origin, dest);
        if (path.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(path);
    }
}