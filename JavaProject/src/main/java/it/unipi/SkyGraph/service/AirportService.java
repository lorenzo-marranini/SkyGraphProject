package it.unipi.SkyGraph.service;

import it.unipi.SkyGraph.dto.*;
import it.unipi.SkyGraph.model.Airport;
import it.unipi.SkyGraph.repository.AirportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AirportService {

    private final AirportRepository airportRepository;

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


}