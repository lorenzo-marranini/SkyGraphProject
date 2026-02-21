package it.unipi.SkyGraph.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.unipi.SkyGraph.dto.*;
import it.unipi.SkyGraph.enums.TimeInterval;
import it.unipi.SkyGraph.model.*;
import it.unipi.SkyGraph.service.CityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    public ResponseEntity<List<CityRankDTO>> getMostTraffickedCities(
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        List<CityRankDTO> connections = cityService.getMostTraffickedCities(limit);
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


    @Operation(summary = "Create or update a City")
    @PostMapping
    public ResponseEntity<City> saveCity(@RequestBody City city) {
        return ResponseEntity.ok(cityService.createOrUpdateCity(city));
    }

    @Operation(summary = "Get all Cities")
    @GetMapping
    public ResponseEntity<Page<City>> getAllCities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return ResponseEntity.ok(cityService.getAllCities(pageable));
    }

    @Operation(summary = "Get a City by its city_state ID")
    @GetMapping("/{cityState}")
    public ResponseEntity<City> getCity(@PathVariable String cityState) {
        try {
            return ResponseEntity.ok(cityService.getCityById(cityState));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Delete a City")
    @DeleteMapping("/{cityState}")
    public ResponseEntity<Void> deleteCity(@PathVariable String cityState) {
        cityService.deleteCity(cityState);
        return ResponseEntity.noContent().build();
    }

}