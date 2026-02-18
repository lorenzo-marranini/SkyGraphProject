package it.unipi.SkyGraph.controller;

import it.unipi.SkyGraph.dto.AirportDTO;
import it.unipi.SkyGraph.dto.AirportRankingDTO;
import it.unipi.SkyGraph.dto.QuickestPathDTO;
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



    @GetMapping("/quickest-path")
    public ResponseEntity<QuickestPathDTO> getQuickestPath(
            @RequestParam String origin,
            @RequestParam String dest,
            @RequestParam(defaultValue = "3") int maxHops) {

        return airportRepository.findQuickestRoute(origin, dest, maxHops)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}