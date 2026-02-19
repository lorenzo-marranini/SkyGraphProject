package it.unipi.SkyGraph.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.unipi.SkyGraph.dto.AirlineStatDTO;
import it.unipi.SkyGraph.dto.AirportStatDTO;
import it.unipi.SkyGraph.dto.FlightDTO;
import it.unipi.SkyGraph.dto.TripItineraryDTO;
import it.unipi.SkyGraph.enums.AirlineSort;
import it.unipi.SkyGraph.enums.AirportSort;
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

    // --- AIRLINE REPRESENTATIVE ENDPOINTS ---

    // 1, 2, 3
    @Operation(summary = "Get airlines ranked by a given metric and time range")
    @GetMapping("/stats/airlines")
    public ResponseEntity<List<AirlineStatDTO>> getAirlineStats(
            @RequestParam AirlineSort sort,
            @RequestParam(defaultValue = "LAST_WEEK") TimeInterval range
    ) {
        List<AirlineStatDTO> result = switch (sort) {
            case DELAY    -> flightService.getAirlinesByAvgDelay(range);
            case FLIGHTS  -> flightService.getAirlinesByTotalFlights(range);
            case TOT_DISTANCE -> flightService.getAirlinesByTotalDistance(range);
            case AVG_DISTANCE -> flightService.getAirlinesByAvgRouteDistance(range);
        };

        return ResponseEntity.ok(result);
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
    /*
    // 8. Efficienza
    @GetMapping("/airlines/efficiency")
    public ResponseEntity<?> getAirlinesByEfficiency(@RequestParam(defaultValue = "LAST_WEEK") String range) {
        return handleAirlineRequest(range, flightService::getAirlinesByEfficiency);
    }
*/

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