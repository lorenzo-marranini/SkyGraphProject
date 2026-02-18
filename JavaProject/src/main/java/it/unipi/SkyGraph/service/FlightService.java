package it.unipi.SkyGraph.service;

import it.unipi.SkyGraph.dto.*;
import it.unipi.SkyGraph.enums.TimeInterval;
import it.unipi.SkyGraph.model.FlightMongo;
import it.unipi.SkyGraph.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
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

    // ------------------ METODI --------------------------------

    // ------------------------ GUEST ------------------------

    // 1)
    public List<FlightMongo> searchFlights(String origin, String destination, String dateString) {

        LocalDate date = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);
        Instant startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        return flightRepository.searchFlights(origin, destination, startOfDay, endOfDay);
    }

    // 2)
    public List<FlightMongo> viewFlightsLive() {


        Instant now = getSimulatedNowInstant();
        Instant startWindow = now.minus(24, ChronoUnit.HOURS);
        Instant endWindow = now.plus(12, ChronoUnit.HOURS);
        // check dei voli live tra 24 ore prima e 12 ore dopo per essere sicuri in casi di ritardi / anticipi

        return flightRepository.searchFlightsLive(startWindow, endWindow );
    }

    //----------------------------------- TRAFFIC CONTROLLER -----------------------

    // 1)
    public List<AirlineStatDTO> getAirlinesByAvgDelay(TimeInterval range) {
        return flightRepository.findAirlinesByAvgDelay(
                calculateMinDate(range),
                getSimulatedNowInstant()
        );
    }

    // 2)
    public List<AirlineStatDTO> getAirlinesByTotalFlights(TimeInterval range) {
        return flightRepository.findAirlinesByTotalFlights(
                calculateMinDate(range),
                getSimulatedNowInstant()
        );
    }

    // 3)
    public List<AirlineStatDTO> getAirlinesByTotalDistance(TimeInterval range) {
        return flightRepository.findAirlinesByTotalDistance(
                calculateMinDate(range),
                getSimulatedNowInstant()
        );
    }

    // 4) TO DO

    // 5)
    public List<AirlineStatDTO> getAirlinesByRoute(String origin, String destination,TimeInterval range) {
        return flightRepository.findAirlinesByRoute(
                origin,
                destination,
                calculateMinDate(range),
                getSimulatedNowInstant()
        );
    }

    // 6)
    public List<AirlineStatDTO> getAirlinesByRouteDelay(String origin, String destination,TimeInterval range) {
        return flightRepository.findAirlinesByRouteDelay(
                origin,
                destination,
                calculateMinDate(range),
                getSimulatedNowInstant()
        );
    }

    // 7)
    public List<AirlineStatDTO> getAirlinesByRouteDistance(String origin, String destination,TimeInterval range) {
        return flightRepository.findAirlinesByAvgRouteDistance(
                calculateMinDate(range),
                getSimulatedNowInstant()
        );
    }

    // 8) TO DO

    // 9) TO DO

//--------------------------- AIRLINE REPRESENTATIVE -------------------------------

    // 1) TO DO

    // 2) TO DO

    // 3) TO DO

    // 4)
    public List<RouteStatsDTO> getRoutesByFlightCount(TimeInterval range) {
        return flightRepository.findRoutesByFlightCount(
                calculateMinDate(range),
                getSimulatedNowInstant()
        );
    }

    // 4.1)
    public List<RouteStatsDTO> getRoutesByCancelledCount(TimeInterval range) {
        return flightRepository.findRoutesByCancelledCount(
                calculateMinDate(range),
                getSimulatedNowInstant()
        );
    }

    // 4.2)
    public List<RouteStatsDTO> getRoutesByDivertedCount(TimeInterval range) {
        return flightRepository.findRoutesByDivertedCount(
                calculateMinDate(range),
                getSimulatedNowInstant()
        );
    }

    // 5)
    public List<DayStatsDTO> getDaysByAvgDelay(TimeInterval range) {
        return flightRepository.findDaysByAvgDelay(
                calculateMinDate(range),
                getSimulatedNowInstant()
        );
    }

    // 6)
    public List<AirlineReportDTO> getAirlineReport(TimeInterval range, String AirlineName) {
        return flightRepository.generateAirlineReport(
                calculateMinDate(range),
                getSimulatedNowInstant(),
                AirlineName
        );
    }

    // 7)
    public List<AirportStatDTO> getAirportsByAvgDelay(TimeInterval range) {
        return flightRepository.findAirportsByAvgDelay(
                calculateMinDate(range),
                getSimulatedNowInstant()
        );
    }


    // --- ALTRI METODI (Updates) ---


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