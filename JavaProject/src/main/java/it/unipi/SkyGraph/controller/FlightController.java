package it.unipi.SkyGraph.controller;

import it.unipi.SkyGraph.dto.AirlineStatDto;
import it.unipi.SkyGraph.repository.FlightRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
public class FlightController {

    private final FlightRepository flightRepository;

    @Autowired
    public FlightController(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }



    // 1. Get airlines ordered by average delay (ascending)

    @GetMapping("/airlines/by-delay")
    public ResponseEntity<List<AirlineStatDto>> getAirlinesByAvgDelay(
            @RequestParam("minDate") String minDate) {
        List<AirlineStatDto> stats = flightRepository.findAirlinesByAvgDelay(minDate);
        return ResponseEntity.ok(stats);
    }

    // 2. Get airlines ordered by total number of flights (descending)
    @GetMapping("/airlines/by-flights")
    public ResponseEntity<List<AirlineStatDto>> getAirlinesByTotalFlights(
            @RequestParam("minDate") String minDate) {
        List<AirlineStatDto> stats = flightRepository.findAirlinesByTotalFlights(minDate);
        return ResponseEntity.ok(stats);
    }

    // 3. Get airlines ordered by total distance traveled (descending)
    // Usage: GET /api/stats/airlines/by-distance?minDate=2023-01-01
    @GetMapping("/airlines/by-distance")
    public ResponseEntity<List<AirlineStatDto>> getAirlinesByTotalDistance(
            @RequestParam("minDate") String minDate) {
        List<AirlineStatDto> stats = flightRepository.findAirlinesByTotalDistance(minDate);
        return ResponseEntity.ok(stats);
    }

    // 4. Get airlines for a specific route ordered by flight volume
    // Usage: GET /api/stats/routes/by-flights?origin=JFK&dest=LHR
    @GetMapping("/routes/by-flights")
    public ResponseEntity<List<AirlineStatDto>> getAirlinesByRouteFlights(
            @RequestParam("origin") String originIata,
            @RequestParam("dest") String destIata) {
        List<AirlineStatDto> stats = flightRepository.findAirlinesByRouteFlights(originIata, destIata);
        if (stats.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(stats);
    }

    // 5. Get airlines for a specific route ordered by average delay
    // Usage: GET /api/stats/routes/by-delay?origin=JFK&dest=LHR
    @GetMapping("/routes/by-delay")
    public ResponseEntity<List<AirlineStatDto>> getAirlinesByRouteDelay(
            @RequestParam("origin") String originIata,
            @RequestParam("dest") String destIata) {
        List<AirlineStatDto> stats = flightRepository.findAirlinesByRouteDelay(originIata, destIata);
        if (stats.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(stats);
    }
}