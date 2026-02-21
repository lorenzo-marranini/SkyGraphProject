package it.unipi.SkyGraph.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.unipi.SkyGraph.dto.*;
import it.unipi.SkyGraph.enums.*;
import it.unipi.SkyGraph.dto.AirlineStatDTO;
import it.unipi.SkyGraph.dto.AirportStatDTO;
import it.unipi.SkyGraph.dto.FlightDTO;
import it.unipi.SkyGraph.dto.TripItineraryDTO;
import it.unipi.SkyGraph.model.FlightMongo;
import it.unipi.SkyGraph.service.FlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;

    private ResponseEntity<Map<String, String>> buildBadRequest(String message) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Invalid request");
        errorResponse.put("message", message);
        return ResponseEntity.badRequest().body(errorResponse);
    }

    // --- 0. RICERCA VOLI ---
    @Operation(summary = "Search flights by origin IATA, destination IATA and date (YYYY-MM-DD)")
    @GetMapping("/flights/search")
    public ResponseEntity<List<FlightDTO>> searchFlights(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam String date
    ) {
        List<FlightDTO> flights = flightService.getFlights(origin, destination, date);
        return flights.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(flights);
    }

    @Operation(summary = "Get live flights")
    @GetMapping("/flights/live")
    public ResponseEntity<Page<FlightDTO>> liveFlights(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("flight_info.schedule.departure_datetime").ascending());
        Page<FlightDTO> flights = flightService.findLiveFlights(pageable);
        return flights.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(flights);
    }


    // 4 da fare su Neo4j



    // -- AIRLINE REPRESENTATIVE

    @Operation(summary = "Get routes ranked by flight count, cancellations or diversions")
    @GetMapping("/routes/rankings")
    public ResponseEntity<List<RouteStatsDTO>> getRouteStats(
            @RequestParam RouteSort sort,
            @RequestParam(defaultValue = "LAST_WEEK") TimeInterval range,
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        List<RouteStatsDTO> result = switch (sort) {
            case FREQUENT  -> flightService.getRoutesByFlightCount(range, limit);
            case CANCELLED -> flightService.getRoutesByCancelledCount(range, limit);
            case DIVERTED  -> flightService.getRoutesByDivertedCount(range, limit);
        };
        return ResponseEntity.ok(result);
    }
    //3



    @Operation(summary = "Get average delay by day of the week in a given time range")
    @GetMapping("/routes/stats/daily-delay")
    public ResponseEntity<List<DayStatsDTO>> getDelayByDayOfWeek(
            @RequestParam(defaultValue = "LAST_WEEK") TimeInterval range
    ) {
        List<DayStatsDTO> result = flightService.getDaysByAvgDelay(range);
        return result.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(result);
    }

    // 7. Top Aeroporti per ritardi
    // --- AIRPORT STATS ---


    @Operation(summary = "Find the quickest actual route checking real flight schedules")
    @GetMapping("/routes/quickest-real")
    public ResponseEntity<List<TripItineraryDTO>> getQuickestRealRoute(
            @RequestParam String origin,
            @RequestParam String dest,
            @RequestParam String date,
            @RequestParam(defaultValue = "2") int maxHops
    ) {
        List<TripItineraryDTO> itineraries = flightService.findQuickestRealRoute(origin, dest, date, maxHops);
        return itineraries.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(itineraries);
    }

    @Operation(summary = "Create a new flight in MongoDB using a simplified DTO and sync the ROUTE to Neo4j")
    @PostMapping("/flights")
    public ResponseEntity<?> createFlight(@RequestBody FlightCreateDTO dto) {
        try {
            FlightMongo savedFlight = flightService.createFlight(dto);
            return ResponseEntity.ok(savedFlight);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Update an existing flight in MongoDB")
    @PutMapping("/flights/{id}")
    public ResponseEntity<FlightMongo> updateFlight(
            @PathVariable String id,
            @RequestBody FlightMongo flight) {
        try {
            return ResponseEntity.ok(flightService.updateFlight(id, flight));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Delete a flight from MongoDB")
    @DeleteMapping("/flights/{id}")
    public ResponseEntity<FlightDTO> deleteFlight(@PathVariable String id) {
        FlightDTO deletedFlight = flightService.deleteFlight(id);
        return ResponseEntity.ok(deletedFlight);
    }
}
