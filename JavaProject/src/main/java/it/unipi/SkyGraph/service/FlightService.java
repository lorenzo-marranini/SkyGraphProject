package it.unipi.SkyGraph.service;

import it.unipi.SkyGraph.dto.AirlineStatDto;
import it.unipi.SkyGraph.dto.AirportStatDto;
import it.unipi.SkyGraph.enums.TimeInterval;
import it.unipi.SkyGraph.model.FlightMongo;
import it.unipi.SkyGraph.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FlightService {

    private final FlightRepository flightRepository;

    private Instant calculateMinDate(TimeInterval range) {
        LocalDate now = LocalDate.of(2026, 2, 25);

        LocalDate calculatedDate;
        switch (range) {
            case LAST_DAY: calculatedDate = now.minusDays(1); break;
            case LAST_WEEK: calculatedDate = now.minusWeeks(1); break;
            case LAST_MONTH: calculatedDate = now.minusMonths(1); break;
            case LAST_YEAR: calculatedDate = now.minusYears(1); break;
            default: calculatedDate = now.minusWeeks(1);
        }

        // Converte in Instant (UTC) inizio giornata
        return calculatedDate.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    // --- 1. RICERCA VOLI (Search) ---
    public List<FlightMongo> searchFlights(String origin, String destination, String dateString) {
        // Parsing data: "2025-08-29" -> StartOfDay e EndOfDay in UTC
        LocalDate date = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);

        Instant startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        return flightRepository.searchFlights(origin, destination, startOfDay, endOfDay);
    }

    // --- METODI STATISTICI ---

    public List<AirlineStatDto> getAirlinesByAvgDelay(TimeInterval range) {
        return flightRepository.findAirlinesByAvgDelay(calculateMinDate(range));
    }
    public List<AirlineStatDto> getAirlinesByTotalFlights(TimeInterval range) {
        return flightRepository.findAirlinesByTotalFlights(calculateMinDate(range));
    }
    public List<AirlineStatDto> getAirlinesByTotalDistance(TimeInterval range) {
        return flightRepository.findAirlinesByTotalDistance(calculateMinDate(range));
    }
    public List<AirlineStatDto> getAirlinesByRouteDelay(String origin, String dest) {
        return flightRepository.findAirlinesByRouteDelay(origin, dest);
    }

    public List<AirlineStatDto> getAirlinesByDiverted(TimeInterval range) {
        return flightRepository.findAirlinesByDiverted(calculateMinDate(range));
    }

    public List<AirlineStatDto> getAirlinesByAvgRouteDistance(TimeInterval range) {
        return flightRepository.findAirlinesByAvgRouteDistance(calculateMinDate(range));
    }

    public List<AirportStatDto> getAirportsByAvgDelay(TimeInterval range) {
        return flightRepository.findAirportsByAvgDelay(calculateMinDate(range));
    }

    public List<AirlineStatDto> getAirlinesByEfficiency(TimeInterval range) {
        return flightRepository.findAirlinesByEfficiency(calculateMinDate(range));
    }
}