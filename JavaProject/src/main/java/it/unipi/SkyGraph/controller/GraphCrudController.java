package it.unipi.SkyGraph.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.unipi.SkyGraph.model.Airport;
import it.unipi.SkyGraph.model.AirportMongo;
import it.unipi.SkyGraph.model.City;
import it.unipi.SkyGraph.service.GraphCrudService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/graph")
@RequiredArgsConstructor
public class GraphCrudController {

    private final GraphCrudService crudService;

    // ================= CITIES =================

    @Operation(summary = "Create or update a City")
    @PostMapping("/cities")
    public ResponseEntity<City> saveCity(@RequestBody City city) {
        return ResponseEntity.ok(crudService.createOrUpdateCity(city));
    }

    @Operation(summary = "Get all Cities")
    @GetMapping("/cities")
    public ResponseEntity<List<City>> getAllCities() {
        return ResponseEntity.ok(crudService.getAllCities());
    }

    @Operation(summary = "Get a City by its city_state ID")
    @GetMapping("/cities/{cityState}")
    public ResponseEntity<City> getCity(@PathVariable String cityState) {
        try {
            return ResponseEntity.ok(crudService.getCityById(cityState));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Delete a City")
    @DeleteMapping("/cities/{cityState}")
    public ResponseEntity<Void> deleteCity(@PathVariable String cityState) {
        crudService.deleteCity(cityState);
        return ResponseEntity.noContent().build();
    }

    // ================= AIRPORTS =================

    @Operation(summary = "Create an Airport and link it to a City via LOCATED_IN")
    @PostMapping("/airports")
    public ResponseEntity<?> createAirport(
            @RequestBody Airport airport,
            @RequestParam String cityName,
            @RequestParam String country
    ) {
        try {
            Airport savedAirport = crudService.createAirport(airport, cityName, country);
            return ResponseEntity.ok(savedAirport);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Update an existing Airport")
    @PutMapping("/airports")
    public ResponseEntity<?> updateAirport(@RequestBody Airport airport) {
        try {
            return ResponseEntity.ok(crudService.updateAirport(airport));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Get all Airports")
    @GetMapping("/airports")
    public ResponseEntity<List<AirportMongo>> getAllAirports() {
        return ResponseEntity.ok(crudService.getAllAirports());
    }

    @Operation(summary = "Get an Airport by IATA code")
    @GetMapping("/airports/{iata}")
    public ResponseEntity<AirportMongo> getAirport(@PathVariable String iata) {
        try {
            return ResponseEntity.ok(crudService.getAirportByIata(iata));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Delete an Airport")
    @DeleteMapping("/airports/{iata}")
    public ResponseEntity<Void> deleteAirport(@PathVariable String iata) {
        crudService.deleteAirport(iata);
        return ResponseEntity.noContent().build();
    }
}