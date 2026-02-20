package it.unipi.SkyGraph.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.unipi.SkyGraph.dto.*;
import it.unipi.SkyGraph.enums.*;
import it.unipi.SkyGraph.dto.AirlineStatDTO;
import it.unipi.SkyGraph.dto.AirportStatDTO;
import it.unipi.SkyGraph.dto.FlightDTO;
import it.unipi.SkyGraph.dto.TripItineraryDTO;
import it.unipi.SkyGraph.service.FlightService;
import lombok.RequiredArgsConstructor;
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
    @GetMapping("/flights")
    public ResponseEntity<List<FlightDTO>> searchFlights(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam String date
    ) {
        List<FlightDTO> flights = flightService.searchFlights(origin, destination, date);
        return flights.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(flights);
    }

    @Operation(summary = "Get live flights")
    @GetMapping("/flights/live")
    public ResponseEntity<List<FlightDTO>> liveFlights() {
        List<FlightDTO> flights = flightService.findLiveFlights();
        return flights.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(flights);
    }

    // --- TRAFFIC CONTROLLER ENDPOINTS ---

    // 1, 2, 3, 7
    @Operation(summary = "Get airlines ranked by a given metric and time range")
    @GetMapping("/stats/airlines")
    public ResponseEntity<List<AirlineStatDTO>> getAirlineStats(
            @RequestParam AirlineSort sort,
            @RequestParam(defaultValue = "LAST_WEEK") TimeInterval range
    ) {
        List<AirlineStatDTO> result = switch (sort) {
            case DELAY        -> flightService.getAirlinesByAvgDelay(range);
            case FLIGHTS      -> flightService.getAirlinesByTotalFlights(range);
            case TOT_DISTANCE -> flightService.getAirlinesByTotalDistance(range);
            case AVG_DISTANCE -> flightService.getAirlinesByAvgRouteDistance(range);
        };
        return ResponseEntity.ok(result);
    }

    // 4 da fare su Neo4j

    // 5, 6
    @Operation(summary = "Get airlines ranked by metric on a specific route")
    @GetMapping("/stats/airlines/route")
    public ResponseEntity<List<AirlineStatDTO>> getAirlineStatsByRoute(
            @RequestParam String originIata,
            @RequestParam String destIata,
            @RequestParam AirlineRouteSort sort,
            @RequestParam(defaultValue = "LAST_WEEK") TimeInterval range
    ) {
        List<AirlineStatDTO> result = switch (sort) {
            case DELAY   -> flightService.getAirlinesByRouteDelay(originIata, destIata, range);
            case FLIGHTS -> flightService.getAirlinesByRoute(originIata, destIata, range);
        };
        return ResponseEntity.ok(result);
    }


    // -- AIRLINE REPRESENTATIVE

    @Operation(summary = "Get routes ranked by flight count, cancellations or diversions")
    @GetMapping("/stats/routes")
    public ResponseEntity<List<RouteStatsDTO>> getRouteStats(
            @RequestParam RouteSort sort,
            @RequestParam(defaultValue = "LAST_WEEK") TimeInterval range
    ) {
        List<RouteStatsDTO> result = switch (sort) {
            case FREQUENT  -> flightService.getRoutesByFlightCount(range);
            case CANCELLED -> flightService.getRoutesByCancelledCount(range);
            case DIVERTED  -> flightService.getRoutesByDivertedCount(range);
        };
        return ResponseEntity.ok(result);
    }
    //3
    @Operation(summary = "Get total flights, departures, and arrivals for a city in a given time range")
    @GetMapping("/stats/cities/{city}")
    public ResponseEntity<CityStatsDTO> getCityStats(
            @PathVariable String city,
            @RequestParam(defaultValue = "LAST_WEEK") TimeInterval range
    ) {
        return ResponseEntity.ok(flightService.getCityHybridStats(city, range));
    }


    @Operation(summary = "Get average delay by day of the week in a given time range")
    @GetMapping("/stats/days/delay")
    public ResponseEntity<List<DayStatsDTO>> getDelayByDayOfWeek(
            @RequestParam(defaultValue = "LAST_WEEK") TimeInterval range
    ) {
        List<DayStatsDTO> result = flightService.getDaysByAvgDelay(range);
        return result.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(result);
    }

    //6.
    @Operation(summary = "Get a comprehensive report for a specific airline in a given time range")
    @GetMapping("/stats/airlines/report")
    public ResponseEntity<List<AirlineReportDTO>> getAirlineReport(
            @RequestParam String airlineName,
            @RequestParam(defaultValue = "LAST_WEEK") TimeInterval range
    ) {
        List<AirlineReportDTO> report = flightService.getAirlineReport(range, airlineName);
        return report.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(report);
    }

    // 7. Top Aeroporti per ritardi
    // --- AIRPORT STATS ---

    @Operation(summary = "Get airports ranked by a given metric and time range")
    @GetMapping("/stats/airports")
    public ResponseEntity<List<AirportStatDTO>> getAirportStats(
            @RequestParam AirportSort sort,
            @RequestParam(defaultValue = "LAST_WEEK") TimeInterval range
    ) {
        List<AirportStatDTO> result = switch (sort) {
            case DELAY -> flightService.getAirportsByAvgDelay(range);
        };
        return ResponseEntity.ok(result);
    }

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
}
