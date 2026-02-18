package it.unipi.SkyGraph.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.unipi.SkyGraph.dto.AirlineStatDTO;
import it.unipi.SkyGraph.dto.AirportStatDTO;
import it.unipi.SkyGraph.enums.TimeInterval;
import it.unipi.SkyGraph.model.FlightMongo;
import it.unipi.SkyGraph.service.FlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;

    // --- 0. RICERCA VOLI ---
    @Operation(summary = "Search flights by origin IATA, destination IATA and date (YYYY-MM-DD)")
    @GetMapping("/search")
    public ResponseEntity<List<FlightMongo>> searchFlights(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam String date
    ) {
        List<FlightMongo> flights = flightService.searchFlights(origin, destination, date);
        if (flights.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(flights);
    }

    // --- AIRLINE REPRESENTATIVE ENDPOINTS ---

    // 1. Ritardo Medio
    @GetMapping("/airlines/by-delay")
    public ResponseEntity<?> getAirlinesByAvgDelay(@RequestParam(defaultValue = "LAST_WEEK") String range) {
        return handleAirlineRequest(range, flightService::getAirlinesByAvgDelay);
    }

    // 2. Voli Totali
    @GetMapping("/airlines/by-flights")
    public ResponseEntity<?> getAirlinesByTotalFlights(@RequestParam(defaultValue = "LAST_WEEK") String range) {
        return handleAirlineRequest(range, flightService::getAirlinesByTotalFlights);
    }

    // 3. Distanza Totale
    @GetMapping("/airlines/by-distance")
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
    // 6. Mean Route Distance per Airline
    @GetMapping("/airlines/by-avg-route-distance")
    public ResponseEntity<?> getAirlinesByAvgRouteDistance(@RequestParam(defaultValue = "LAST_WEEK") String range) {
        return handleAirlineRequest(range, flightService::getAirlinesByAvgRouteDistance);
    }

    // 7. Top Aeroporti per ritardi
    @GetMapping("/airports/by-delay")
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
}