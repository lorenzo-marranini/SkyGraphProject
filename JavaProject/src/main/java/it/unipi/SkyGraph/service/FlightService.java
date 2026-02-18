package it.unipi.SkyGraph.service;

import it.unipi.SkyGraph.dto.AirlineStatDto;
import it.unipi.SkyGraph.dto.AirportStatDto;
import it.unipi.SkyGraph.dto.FlightLogDTO;
import it.unipi.SkyGraph.enums.TimeInterval;
import it.unipi.SkyGraph.model.FlightMongo;
import it.unipi.SkyGraph.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
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
    private final MongoTemplate mongoTemplate;

    // Definiamo la data di riferimento "ADESSO" statica per la simulazione
    private static final LocalDate SIMULATED_NOW = LocalDate.of(2026, 2, 25);

    /**
     * Calcola la data di inizio (minDate) basata sull'intervallo richiesto
     * rispetto alla data simulata "SIMULATED_NOW".
     */
    private Instant calculateMinDate(TimeInterval range) {
        LocalDate calculatedDate;
        switch (range) {
            case LAST_DAY: calculatedDate = SIMULATED_NOW.minusDays(1); break;
            case LAST_WEEK: calculatedDate = SIMULATED_NOW.minusWeeks(1); break;
            case LAST_MONTH: calculatedDate = SIMULATED_NOW.minusMonths(1); break;
            case LAST_YEAR: calculatedDate = SIMULATED_NOW.minusYears(1); break;
            default: calculatedDate = SIMULATED_NOW.minusWeeks(1);
        }
        return calculatedDate.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    /**
     * Restituisce la fine della giornata "simulata" corrente (25 Feb 2026 23:59:59)
     * Usato come limite superiore (maxDate) per le query.
     */
    private Instant getSimulatedNowInstant() {
        // Prendiamo la fine della giornata corrente o l'inizio del giorno dopo
        return SIMULATED_NOW.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    // --- METODI STATISTICI ---

    public List<AirlineStatDto> getAirlinesByAvgDelay(TimeInterval range) {
        return flightRepository.findAirlinesByAvgDelay(
                calculateMinDate(range),
                getSimulatedNowInstant()
        );
    }

    public List<AirlineStatDto> getAirlinesByTotalFlights(TimeInterval range) {
        return flightRepository.findAirlinesByTotalFlights(
                calculateMinDate(range),
                getSimulatedNowInstant()
        );
    }

    public List<AirlineStatDto> getAirlinesByTotalDistance(TimeInterval range) {
        return flightRepository.findAirlinesByTotalDistance(
                calculateMinDate(range),
                getSimulatedNowInstant()
        );
    }
/*
    public List<AirlineStatDto> getAirlinesByDiverted(TimeInterval range) {
        return flightRepository.findAirlinesByDiverted(
                calculateMinDate(range),
                getSimulatedNowInstant()
        );
    }
*/
    public List<AirlineStatDto> getAirlinesByAvgRouteDistance(TimeInterval range) {
        return flightRepository.findAirlinesByAvgRouteDistance(
                calculateMinDate(range),
                getSimulatedNowInstant()
        );
    }

    public List<AirportStatDto> getAirportsByAvgDelay(TimeInterval range) {
        return flightRepository.findAirportsByAvgDelay(
                calculateMinDate(range),
                getSimulatedNowInstant()
        );
    }
/*
    public List<AirlineStatDto> getAirlinesByEfficiency(TimeInterval range) {
        return flightRepository.findAirlinesByEfficiency(
                calculateMinDate(range),
                getSimulatedNowInstant()
        );
    }
*/
    // --- ALTRI METODI (Guest / Updates) ---

    public List<FlightMongo> searchFlights(String origin, String destination, String dateString) {
        LocalDate date = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);
        Instant startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return flightRepository.searchFlights(origin, destination, startOfDay, endOfDay);
    }

    public void updateFlightLog(FlightLogDTO dto) {
        Query query = new Query(Criteria.where("flight_info.flight_key").is(dto.getFlightKey()));
        FlightMongo.FlightLog log = new FlightMongo.FlightLog(
                new FlightMongo.GeoLocation("Point", List.of(dto.getLon(), dto.getLat())),
                dto.getAlt(),
                dto.getGspeed(),
                dto.getTimestamp(),
                dto.getEta()
        );
        Update update = new Update().set("flight_log", log);
        mongoTemplate.updateFirst(query, update, FlightMongo.class);
    }
}