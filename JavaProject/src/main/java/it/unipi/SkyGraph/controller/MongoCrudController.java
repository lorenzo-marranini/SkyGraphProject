package it.unipi.SkyGraph.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.unipi.SkyGraph.model.FlightMongo;
import it.unipi.SkyGraph.service.MongoCrudService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mongo/flights")
@RequiredArgsConstructor
public class MongoCrudController {

    private final MongoCrudService mongoCrudService;

    @Operation(summary = "Create a new flight in MongoDB and sync the ROUTE to Neo4j")
    @PostMapping
    public ResponseEntity<FlightMongo> createFlight(@RequestBody FlightMongo flight) {
        try {
            FlightMongo savedFlight = mongoCrudService.createFlight(flight);
            return ResponseEntity.ok(savedFlight);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Get a flight by its MongoDB ObjectId")
    @GetMapping("/{id}")
    public ResponseEntity<FlightMongo> getFlight(@PathVariable String id) {
        try {
            return ResponseEntity.ok(mongoCrudService.getFlightById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Update an existing flight in MongoDB")
    @PutMapping("/{id}")
    public ResponseEntity<FlightMongo> updateFlight(
            @PathVariable String id,
            @RequestBody FlightMongo flight) {
        try {
            return ResponseEntity.ok(mongoCrudService.updateFlight(id, flight));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Delete a flight from MongoDB")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFlight(@PathVariable String id) {
        mongoCrudService.deleteFlight(id);
        return ResponseEntity.noContent().build();
    }
}