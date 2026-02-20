package it.unipi.SkyGraph.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.unipi.SkyGraph.dto.*;
import it.unipi.SkyGraph.enums.AirportSort;
import it.unipi.SkyGraph.enums.TimeInterval;
import it.unipi.SkyGraph.model.Airport;
import it.unipi.SkyGraph.service.AirportService;
import it.unipi.SkyGraph.service.FlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/airports")
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


    @Operation(summary = "Get best emergency lading airports for a specific flight in air")
    @GetMapping("/emergency")
    public ResponseEntity<List<AirportEmergencyDTO>> getEmergencyAirports(
            @RequestParam String flightKey) {
        List<AirportEmergencyDTO> nearestAirports = airportService.getNearestAirportsForEmergency(flightKey);
        if (nearestAirports.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(nearestAirports);
    }

    @Operation(summary = "Get best alternative airports for a closed airport")
    @GetMapping("/{iata}/alternative")
    public ResponseEntity<List<AirportRankingDTO>> getAlternativeAirports(
            @PathVariable String iata) {
        List<AirportRankingDTO> alternatives = airportService.getBestAlternativeAirports(iata);
        if (alternatives.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(alternatives);
    }

    // --- AIRLINE REPRESENTATIVE ---

    @Operation(summary = "Get top hubs by PageRank centrality score")
    @GetMapping("/rankings/hubs")
    public ResponseEntity<List<AirportRankingDTO>> getTopHubs() {
        List<AirportRankingDTO> hubs = airportService.getTopHubsByRank();
        if (hubs.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(hubs);
    }

    @Operation(summary = "Get airports ranked by a given metric and time range")
    @GetMapping("/rankings")
    public ResponseEntity<List<AirportStatDTO>> getAirportStats(
            @RequestParam AirportSort sort,
            @RequestParam(defaultValue = "LAST_WEEK") TimeInterval range
    ) {
        List<AirportStatDTO> result = switch (sort) {
            case DELAY -> airportService.getAirportsByAvgDelay(range);
        };
        return ResponseEntity.ok(result);
    }

    /* AR 3 DA CANCELLARE

    @Operation(summary = "Get the quickest route between two airports based on scheduled time")

    @GetMapping("/connections/quickest")
    public ResponseEntity<QuickestPathDTO> getQuickestRoute(
            @RequestParam String origin,
            @RequestParam String dest,
            @RequestParam(defaultValue = "3") int maxHops) {
        return airportService.getQuickestRoute(origin, dest, maxHops)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
    */
}