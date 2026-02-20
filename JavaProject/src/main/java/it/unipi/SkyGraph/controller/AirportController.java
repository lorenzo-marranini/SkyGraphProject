package it.unipi.SkyGraph.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.unipi.SkyGraph.dto.AirportRankingDTO;
import it.unipi.SkyGraph.dto.QuickestPathDTO;
import it.unipi.SkyGraph.model.Airport;
import it.unipi.SkyGraph.service.AirportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/neo4j/airports")
@RequiredArgsConstructor
public class AirportController {

    private final AirportService airportService;

    // --- GUEST ---

    // --- TRAFFIC CONTROLLER ---
    @Operation(summary = "Get airports ranked by number of routes")
    @GetMapping("/rankings/connections")
    public ResponseEntity<List<AirportRankingDTO>> getAirportsConnections() {
        List<AirportRankingDTO> connections = airportService.getAirportsConnections();
        if (connections.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(connections);
    }

    @Operation(summary = "Get best alternative airports for a closed airport")
    @GetMapping("/alternative")
    public ResponseEntity<List<AirportRankingDTO>> getAlternativeAirports(
            @RequestParam String closedIata) {
        List<AirportRankingDTO> alternatives = airportService.getBestAlternativeAirports(closedIata);
        if (alternatives.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(alternatives);
    }

    // --- AIRLINE REPRESENTATIVE ---
    @Operation(summary = "Get the quickest route between two airports based on scheduled time")
    @GetMapping("/routes/quickest")
    public ResponseEntity<QuickestPathDTO> getQuickestRoute(
            @RequestParam String origin,
            @RequestParam String dest,
            @RequestParam(defaultValue = "3") int maxHops) {
        return airportService.getQuickestRoute(origin, dest, maxHops)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @Operation(summary = "Get top hubs by PageRank centrality score")
    @GetMapping("/rankings/hubs")
    public ResponseEntity<List<AirportRankingDTO>> getTopHubs() {
        List<AirportRankingDTO> hubs = airportService.getTopHubsByRank();
        if (hubs.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(hubs);
    }
}