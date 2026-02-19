package it.unipi.SkyGraph.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.unipi.SkyGraph.dto.*;
import it.unipi.SkyGraph.enums.AirlineSort;
import it.unipi.SkyGraph.dto.AirlineStatDTO;
import it.unipi.SkyGraph.dto.AirportStatDTO;
import it.unipi.SkyGraph.dto.FlightDTO;
import it.unipi.SkyGraph.dto.TripItineraryDTO;
import it.unipi.SkyGraph.enums.AirportSort;
import it.unipi.SkyGraph.enums.TimeInterval;
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
        if (flights.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(flights);
    }

    @Operation(summary = "Get live flights")
    @GetMapping("/flights/live")
    public ResponseEntity<List<FlightDTO>> liveFlights() {
        List<FlightDTO> flights = flightService.findLiveFlights();
        if (flights.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(flights);
    }

    // --- TRAFFIC CONTROLLER ENDPOINTS ---

    // 1, 2, 3, 7
    @Operation(summary = "Get airlines ranked by a given metric and time range")
    @GetMapping("/stats/airlines")
    public ResponseEntity<?> getAirlineStats(
            @RequestParam String sort,
            @RequestParam(defaultValue = "LAST_WEEK") String range
    ) {
        // Normalizziamo l'input (opzionale, ma consigliato per essere case-insensitive)
        String sortUpper = sort.toUpperCase();

        try {
            List<AirlineStatDTO> result = switch (sortUpper) {
                case "DELAY"        -> flightService.getAirlinesByAvgDelay(range);
                case "FLIGHTS"      -> flightService.getAirlinesByTotalFlights(range);
                case "TOT_DISTANCE" -> flightService.getAirlinesByTotalDistance(range);
                case "AVG_DISTANCE" -> flightService.getAirlinesByAvgRouteDistance(range);
                default -> throw new IllegalArgumentException("Invalid sort parameter. Value '" + sort + "' is not supported. Use: DELAY, FLIGHTS, TOT_DISTANCE, AVG_DISTANCE.");
            };

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            return buildBadRequest(e.getMessage());
        }
    }

    // 4 da fare su Neo4j

    // 5, 6
    @Operation(summary = "Get airlines ranked by a given metric and time range on a specific route")
    @GetMapping("/stats/airlines/route")
    public ResponseEntity<?> getAirlineStatsByRoute(
            @RequestParam String origin_iata,
            @RequestParam String dest_iata,
            @RequestParam String sort, // Riceviamo una String generica
            @RequestParam(defaultValue = "LAST_WEEK") String range
    ) {
        String sortUpper = sort.toUpperCase();

        try {
            List<AirlineStatDTO> result = switch (sortUpper) {
                case "DELAY"        -> flightService.getAirlinesByRouteDelay(origin_iata, dest_iata, range);
                case "FLIGHTS"      -> flightService.getAirlinesByRoute(origin_iata, dest_iata, range);
                // La sintassi corretta per il default nello switch expression
                default -> throw new IllegalArgumentException("Invalid sort parameter. Value '" + sort + "' is not supported. Use: DELAY, FLIGHTS.");
            };

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            return buildBadRequest(e.getMessage());
        }
    }


    // -- AIRLINE REPRESENTATIVE

    @Operation(summary = "Get the most frequent routes in a given time range")
    @GetMapping("/stats/routes/frequent")
    public ResponseEntity<?> getFrequentRoutes(
            @RequestParam(defaultValue = "LAST_WEEK") String range
    ) {
        try {
            List<RouteStatsDTO> result = flightService.getRoutesByFlightCount(range);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return buildBadRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get the routes with the most cancelled flights in a given time range")
    @GetMapping("/stats/routes/cancelled")
    public ResponseEntity<?> getCancelledRoutes(
            @RequestParam(defaultValue = "LAST_WEEK") String range
    ) {
        try {
            List<RouteStatsDTO> result = flightService.getRoutesByCancelledCount(range);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return buildBadRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get the routes with the most diverted flights in a given time range")
    @GetMapping("/stats/routes/diverted")
    public ResponseEntity<?> getDivertedRoutes(
            @RequestParam(defaultValue = "LAST_WEEK") String range
    ) {
        try {
            List<RouteStatsDTO> result = flightService.getRoutesByDivertedCount(range);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return buildBadRequest(e.getMessage());
        }
    }
    //3
    @Operation(summary = "Get total flights, departures, and arrivals for a city in a given time range")
    @GetMapping("/stats/cities/{city}")
    public ResponseEntity<?> getCityStats(
            @PathVariable String city,
            @RequestParam(defaultValue = "LAST_WEEK") String range
    ) {
        try {
            CityStatsDTO stats = flightService.getCityHybridStats(city, range);
            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException e) {
            return buildBadRequest(e.getMessage());
        }
    }


    @Operation(summary = "Get average delay by day of the week in a given time range")
    @GetMapping("/stats/days/delay")
    public ResponseEntity<?> getDelayByDayOfWeek(
            @RequestParam(defaultValue = "LAST_WEEK") String range
    ) {
        try {
            List<DayStatsDTO> result = flightService.getDaysByAvgDelay(range);
            if (result.isEmpty()) return ResponseEntity.noContent().build();
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return buildBadRequest(e.getMessage());
        }
    }

    //6.
    @Operation(summary = "Get a comprehensive report for a specific airline in a given time range")
    @GetMapping("/stats/airlines/report")
    public ResponseEntity<?> getAirlineReport(
            @RequestParam String airlineName,
            @RequestParam(defaultValue = "LAST_WEEK") String range
    ) {
        try {
            // Richiama il metodo che hai già preparato nel Service
            List<AirlineReportDTO> report = flightService.getAirlineReport(range, airlineName);

            if (report.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(report);

        } catch (IllegalArgumentException e) {
            return buildBadRequest(e.getMessage());
        }
    }
    /*
    // 6. Mean Route Distance per Airline
    @GetMapping("/airlines/by-avg-route-distance")
    public ResponseEntity<?> getAirlinesByAvgRouteDistance(@RequestParam(defaultValue = "LAST_WEEK") String range) {
        return handleAirlineRequest(range, flightService::getAirlinesByAvgRouteDistance);
    }*/

    // 7. Top Aeroporti per ritardi
    // --- AIRPORT STATS ---
    @Operation(summary = "Get airports ranked by a given metric and time range")
    @GetMapping("/stats/airports")
    public ResponseEntity<?> getAirportStats(
            @RequestParam String sort,
            @RequestParam(defaultValue = "LAST_WEEK") String range
    ) {
        String sortUpper = sort.toUpperCase();
        try {
            List<AirportStatDTO> result = switch (sortUpper) {
                case "DELAY" -> flightService.getAirportsByAvgDelay(range);
                default -> throw new IllegalArgumentException("Invalid sort parameter. Value '" + sort + "' is not supported. Use: DELAY.");
            };

            return ResponseEntity.ok(result);

    } catch (IllegalArgumentException e) {
        return buildBadRequest(e.getMessage());
    }
    }



    @Operation(summary = "Find the quickest actual route checking real flight schedules")
    @GetMapping("/routes/quickest-real")
    public ResponseEntity<List<TripItineraryDTO>> getQuickestRealRoute(
            @RequestParam String origin,
            @RequestParam String dest,
            @RequestParam String date, // YYYY-MM-DD
            @RequestParam(defaultValue = "2") int maxHops
    ) {
        List<TripItineraryDTO> itineraries = flightService.findQuickestRealRoute(origin, dest, date, maxHops);

        if (itineraries.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(itineraries);
    }
}
