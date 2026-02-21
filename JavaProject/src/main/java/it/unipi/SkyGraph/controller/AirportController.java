package it.unipi.SkyGraph.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.unipi.SkyGraph.dto.*;
import it.unipi.SkyGraph.enums.AirportSort;
import it.unipi.SkyGraph.enums.TimeInterval;
import it.unipi.SkyGraph.model.Airport;
import it.unipi.SkyGraph.model.AirportMongo;
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
    public ResponseEntity<List<AirportRankingDTO>> getAirportsConnections(
           @RequestParam(defaultValue = "10") Integer limit
    ) {
        List<AirportRankingDTO> connections = airportService.getAirportsConnections(limit);
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

    @Operation(summary = "Get top hubs by centrality score")
    @GetMapping("/rankings/hubs")
    public ResponseEntity<List<AirportRankingDTO>> getTopHubs(
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        List<AirportRankingDTO> hubs = airportService.getTopHubsByRank(limit);
        if (hubs.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(hubs);
    }

    @Operation(summary = "Get airports ranked by a given metric and time range")
    @GetMapping("/rankings")
    public ResponseEntity<List<AirportStatDTO>> getAirportStats(
            @RequestParam AirportSort sort,
            @RequestParam(defaultValue = "LAST_WEEK") TimeInterval range,
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        List<AirportStatDTO> result = switch (sort) {
            case DELAY -> airportService.getAirportsByAvgDelay(range, limit);
        };
        return ResponseEntity.ok(result);
    }



    @Operation(summary = "Create an Airport and link it to a City via LOCATED_IN")
    @PostMapping("/airports")
    public ResponseEntity<?> createAirport(
            @RequestBody Airport airport,
            @RequestParam String cityName,
            @RequestParam String country
    ) {
        try {
            Airport savedAirport = airportService.createAirport(airport, cityName, country);
            return ResponseEntity.ok(savedAirport);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Update an existing Airport")
    @PutMapping("/airports")
    public ResponseEntity<?> updateAirport(@RequestBody Airport airport) {
        try {
            return ResponseEntity.ok(airportService.updateAirport(airport));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Get all Airports")
    @GetMapping("/airports")
    public ResponseEntity<List<AirportMongo>> getAllAirports() {
        return ResponseEntity.ok(airportService.getAllAirports());
    }

    @Operation(summary = "Get an Airport by IATA code")
    @GetMapping("/airports/{iata}")
    public ResponseEntity<AirportMongo> getAirport(@PathVariable String iata) {
        try {
            return ResponseEntity.ok(airportService.getExistingAirportByIata(iata));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Delete an Airport")
    @DeleteMapping("/airports/{iata}")
    public ResponseEntity<Void> deleteAirport(@PathVariable String iata) {
        airportService.deleteAirport(iata);
        return ResponseEntity.noContent().build();
    }

}