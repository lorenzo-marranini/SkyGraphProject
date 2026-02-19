package it.unipi.SkyGraph.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.unipi.SkyGraph.dto.AirlineStatDTO;
import it.unipi.SkyGraph.dto.AirportStatDTO;
import it.unipi.SkyGraph.dto.FlightDTO;
import it.unipi.SkyGraph.dto.TripItineraryDTO;
import it.unipi.SkyGraph.enums.TimeInterval;
import it.unipi.SkyGraph.service.FlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;

    // --- 0. RICERCA VOLI ---
    @Operation(summary = "Search flights by origin IATA, destination IATA and date (YYYY-MM-DD)")
    @GetMapping("/flights/search")
    public ResponseEntity<List<FlightDTO>> searchFlights(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam String date
    ) {
        List<FlightDTO> flights = flightService.searchFlights(origin, destination, date);
        if (flights.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(flights);
    }

    @Operation(summary = "Search flights by origin IATA, destination IATA and date (YYYY-MM-DD)")
    @GetMapping("/flights/live")
    public ResponseEntity<List<FlightDTO>> liveFlights() {
        List<FlightDTO> flights = flightService.findLiveFlights();
        if (flights.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(flights);
    }

    // --- AIRLINE REPRESENTATIVE ENDPOINTS ---

    // 1. Ritardo Medio
    @GetMapping("/stats/airlines/by-delay")
    public ResponseEntity<?> getAirlinesByAvgDelay(@RequestParam(defaultValue = "LAST_WEEK") String range) {
        return handleAirlineRequest(range, flightService::getAirlinesByAvgDelay);
    }

    // 2. Voli Totali
    @GetMapping("/stats/airlines/by-flights")
    public ResponseEntity<?> getAirlinesByTotalFlights(@RequestParam(defaultValue = "LAST_WEEK") String range) {
        return handleAirlineRequest(range, flightService::getAirlinesByTotalFlights);
    }

    // 3. Distanza Totale
    @GetMapping("/stats/airlines/by-distance")
    public ResponseEntity<?> getAirlinesByTotalDistance(@RequestParam(defaultValue = "LAST_WEEK") String range) {
        return handleAirlineRequest(range, flightService::getAirlinesByTotalDistance);
    }

    // 4. Ritardo medio su rotta
//    @GetMapping("/routes/by-delay")
//    public ResponseEntity<List<AirlineStatDto>> getAirlinesByRouteDelay(
//            @RequestParam String origin, @RequestParam String dest) {
//        return ResponseEntity.ok(flightService.getAirlinesByRouteDelay(origin, dest));
//    }
    /*
    // 5. Deviazioni
    @GetMapping("/airlines/by-diverted")
    public ResponseEntity<?> getAirlinesByDiverted(@RequestParam(defaultValue = "LAST_WEEK") String range) {
        return handleAirlineRequest(range, flightService::getAirlinesByDiverted);
    }
*/
    /*
    // 6. Mean Route Distance per Airline
    @GetMapping("/airlines/by-avg-route-distance")
    public ResponseEntity<?> getAirlinesByAvgRouteDistance(@RequestParam(defaultValue = "LAST_WEEK") String range) {
        return handleAirlineRequest(range, flightService::getAirlinesByAvgRouteDistance);
    }*/

    // 7. Top Aeroporti per ritardi
    @GetMapping("/stats/airports/by-delay")
    public ResponseEntity<?> getAirportsByAvgDelay(@RequestParam(defaultValue = "LAST_WEEK") String range) {
        try {
            TimeInterval interval = TimeInterval.valueOf(range.toUpperCase());
            List<AirportStatDTO> stats = flightService.getAirportsByAvgDelay(interval);
            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid time range");
        }
    }
    /*
    // 8. Efficienza
    @GetMapping("/airlines/efficiency")
    public ResponseEntity<?> getAirlinesByEfficiency(@RequestParam(defaultValue = "LAST_WEEK") String range) {
        return handleAirlineRequest(range, flightService::getAirlinesByEfficiency);
    }
*/
    private ResponseEntity<?> handleAirlineRequest(String range, java.util.function.Function<TimeInterval, List<AirlineStatDTO>> serviceMethod) {
        try {
            TimeInterval interval = TimeInterval.valueOf(range.toUpperCase());
            return ResponseEntity.ok(serviceMethod.apply(interval));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid time range. Allowed: LAST_DAY, LAST_WEEK, LAST_MONTH, LAST_YEAR");
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