package it.unipi.SkyGraph.service;

import it.unipi.SkyGraph.dto.AirportRankingDTO;
import it.unipi.SkyGraph.dto.QuickestPathDTO;
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
    public List<AirportRankingDTO> getAirportsConnections() {
        return airportRepository.findAirportsConnections();
    }

    public List<AirportRankingDTO> getBestAlternativeAirports(String closedIata) {
        return airportRepository.findBestAlternativeAirports(closedIata);
    }

    // --- AIRLINE REPRESENTATIVE ---
    public Optional<QuickestPathDTO> getQuickestRoute(String origin, String dest, int maxHops) {
        return airportRepository.findQuickestRoute(origin, dest, maxHops);
    }

    public List<AirportRankingDTO> getTopHubsByPageRank() {
        return airportRepository.findTopHubsByPageRank();
    }

}