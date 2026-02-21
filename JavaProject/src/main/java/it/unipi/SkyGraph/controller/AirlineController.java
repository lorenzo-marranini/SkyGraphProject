package it.unipi.SkyGraph.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.models.security.SecurityScheme;
import it.unipi.SkyGraph.dto.AirlineReportDTO;
import it.unipi.SkyGraph.dto.AirlineStatDTO;
import it.unipi.SkyGraph.enums.AirlineRouteSort;
import it.unipi.SkyGraph.enums.AirlineSort;
import it.unipi.SkyGraph.enums.TimeInterval;
import it.unipi.SkyGraph.service.AirlineService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/airlines")
@RequiredArgsConstructor
public class AirlineController {

    private final AirlineService airlineService;

    // --- TRAFFIC CONTROLLER ENDPOINTS ---

    // 1, 2, 3, 7
    @Operation(summary = "Get airlines ranked by a given metric and time range")
    @GetMapping("/rankings")
    public ResponseEntity<List<AirlineStatDTO>> getAirlineStats(
            @RequestParam AirlineSort sort,
            @RequestParam(defaultValue = "LAST_WEEK") TimeInterval range,
            @RequestParam(defaultValue = "10") Integer limit
            ) {
        List<AirlineStatDTO> result = switch (sort) {
            case DELAY        -> airlineService.getAirlinesByAvgDelay(range, limit);
            case FLIGHTS      -> airlineService.getAirlinesByTotalFlights(range, limit);
            case TOT_DISTANCE -> airlineService.getAirlinesByTotalDistance(range, limit);
            case AVG_DISTANCE -> airlineService.getAirlinesByAvgRouteDistance(range, limit);
        };
        return ResponseEntity.ok(result);
    }


    // 5, 6
    @Operation(summary = "Get airlines ranked by metric on a specific route")
    @GetMapping("/rankings/by-route")
    public ResponseEntity<List<AirlineStatDTO>> getAirlineStatsByRoute(
            @RequestParam String originIata,
            @RequestParam String destIata,
            @RequestParam AirlineRouteSort sort,
            @RequestParam(defaultValue = "LAST_WEEK") TimeInterval range,
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        List<AirlineStatDTO> result = switch (sort) {
            case DELAY   -> airlineService.getAirlinesByRouteDelay(originIata, destIata, range, limit);
            case FLIGHTS -> airlineService.getAirlinesByRoute(originIata, destIata, range, limit);
        };
        return ResponseEntity.ok(result);
    }


    //6.
    @Operation(summary = "Get a comprehensive report for a specific airline in a given time range")
    @GetMapping("/{airlineIata}/report")
    public ResponseEntity<List<AirlineReportDTO>> getAirlineReport(
            @PathVariable String airlineIata,
            @RequestParam(defaultValue = "LAST_WEEK") TimeInterval range
    ) {
        List<AirlineReportDTO> report = airlineService.getAirlineReport(range, airlineIata);
        return report.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(report);
    }
}
