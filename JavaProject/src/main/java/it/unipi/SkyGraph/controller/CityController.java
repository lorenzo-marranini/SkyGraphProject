package it.unipi.SkyGraph.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.unipi.SkyGraph.dto.*;
import it.unipi.SkyGraph.model.*;
import it.unipi.SkyGraph.service.CityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CityController {

    private final CityService cityService;


    // --------------- AIRLINE REPRESENTATIVE -------------

    @Operation(summary = "Get most trafficked cities")
    @GetMapping("/stats/cities/connections")
    public ResponseEntity<List<CityStatsDTO>> getMostTraffickedCities() {
        List<CityStatsDTO> connections = cityService.getMostTraffickedCities();
        if (connections.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(connections);
    }

}