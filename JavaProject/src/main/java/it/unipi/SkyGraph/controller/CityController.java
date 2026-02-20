package it.unipi.SkyGraph.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.unipi.SkyGraph.dto.*;
import it.unipi.SkyGraph.enums.TimeInterval;
import it.unipi.SkyGraph.model.*;
import it.unipi.SkyGraph.service.CityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cities")
@RequiredArgsConstructor
public class CityController {

    private final CityService cityService;

    // --------------- AIRLINE REPRESENTATIVE -------------

    @Operation(summary = "Get most trafficked cities")
    @GetMapping("/rankings/traffic")
    public ResponseEntity<List<CityRankDTO>> getMostTraffickedCities() {
        List<CityRankDTO> connections = cityService.getMostTraffickedCities();
        if (connections.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(connections);
    }

    @Operation(summary = "Get total flights, departures, and arrivals for a city in a given time range")
    @GetMapping("/{city}/stats")
    public ResponseEntity<CityStatsDTO> getCityStats(
            @PathVariable String city,
            @RequestParam(defaultValue = "LAST_WEEK") TimeInterval range
    ) {
        return ResponseEntity.ok(cityService.getCityHybridStats(city, range));
    }

}