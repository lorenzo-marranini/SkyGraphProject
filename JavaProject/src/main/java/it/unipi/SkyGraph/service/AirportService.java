package it.unipi.SkyGraph.service;

import it.unipi.SkyGraph.config.SimulationClock;
import it.unipi.SkyGraph.dto.*;
import it.unipi.SkyGraph.enums.TimeInterval;
import it.unipi.SkyGraph.model.Airport;
import it.unipi.SkyGraph.repository.AirportRepository;
import it.unipi.SkyGraph.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AirportService {

    private final AirportRepository airportRepository;
    private final FlightRepository flightRepository;
    private final SimulationClock clock;

    // --- GUEST ---
    public Optional<Airport> getAirportByIata(String iataCode) {
        return airportRepository.findByIataCode(iataCode);
    }

    // --- TRAFFIC CONTROLLER ---

    // 4)
    public List<AirportRankingDTO> getAirportsConnections() {
        List<AirportRankingDTO> result = airportRepository.findAirportsConnections();
        result.forEach(dto  -> dto.setScoreType("NUMBER_OF_CONNECTIONS"));

        return result;
    }

    // 9)
    public List<AirportRankingDTO> getBestAlternativeAirports(String closedIata) {
        List<AirportRankingDTO> result = airportRepository.findBestAlternativeAirports(closedIata);
        result.forEach(dto  -> dto.setScoreType("SHARED_CONNECTIONS/KM_DISTANCE"));
        return result;
    }



    // --- AIRLINE REPRESENTATIVE ---

    // 1)
    public Optional<QuickestPathDTO> getQuickestRoute(String origin, String dest, int maxHops) {
        return airportRepository.findQuickestRoute(origin, dest, maxHops);
    }

    // 2)
    public List<AirportRankingDTO> getTopHubsByRank() {
        List<AirportRankingDTO> result = airportRepository.findTopHubsByRank();
        result.forEach(dto  -> dto.setScoreType("BETWEENNESS_SCORE"));
        return result;
    }


    // 7)
    public List<AirportStatDTO> getAirportsByAvgDelay(TimeInterval range) {
        List<AirportStatDTO> result =  flightRepository.findAirportsByAvgDelay(
                clock.calculateMinDate(range),
                clock.getSimulatedNowInstant()
        );

        result.forEach(dto  -> dto.setScoreType("AVG_DELAY_MIN"));
        return result;
    }

    // 5)

}