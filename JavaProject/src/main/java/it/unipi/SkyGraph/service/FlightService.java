package it.unipi.SkyGraph.service;

import it.unipi.SkyGraph.dto.*;
import it.unipi.SkyGraph.enums.TimeInterval;
import it.unipi.SkyGraph.model.FlightMongo;
import it.unipi.SkyGraph.repository.AirportRepository;
import it.unipi.SkyGraph.repository.CityRepository;
import it.unipi.SkyGraph.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlightService {

    private final FlightRepository flightRepository;
    private final AirportRepository airportRepository;
    private final CityRepository cityRepository;
    private final MongoTemplate mongoTemplate;

    // Definiamo la data di riferimento "ADESSO" statica per la simulazione
    private static final LocalDate SIMULATED_NOW = LocalDate.of(2026, 2, 25);

    /**
     * Calcola la data di inizio (minDate) basata sull'intervallo richiesto
     * rispetto alla data simulata "SIMULATED_NOW".
     */
    private Instant calculateMinDate(TimeInterval range) {
        LocalDate calculatedDate = switch (range) {
            case LAST_DAY   -> SIMULATED_NOW.minusDays(1);
            case LAST_WEEK  -> SIMULATED_NOW.minusWeeks(1);
            case LAST_MONTH -> SIMULATED_NOW.minusMonths(1);
            case LAST_YEAR  -> SIMULATED_NOW.minusYears(1);
        };
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

    /**
     * Transforms a FlightMongo Obj in a FlightDTO
     */
    private FlightDTO convertToDTO(FlightMongo flight) {

        var coordinates = flight.getFlightLog().getLocation().getCoordinates();

        return FlightDTO.builder()
                .flightId(flight.getFlightInfo().getFlightKey())

                .airline(FlightDTO.Airline.builder()
                        .iata(flight.getFlightInfo().getAirline().getIata())
                        .name(flight.getFlightInfo().getAirline().getName())
                        .build())

                .route(FlightDTO.Route.builder()
                        .origin(FlightDTO.Airport.builder()
                                .iata(flight.getRoute().getOrigin().getIata())
                                .city(flight.getRoute().getOrigin().getCity())
                                .build())
                        .destination(FlightDTO.Airport.builder()
                                .iata(flight.getRoute().getDestination().getIata())
                                .city(flight.getRoute().getDestination().getCity())
                                .build())
                        .build())

                .schedule(FlightDTO.Schedule.builder()
                        .departureUtc(flight.getFlightInfo().getSchedule().getDepartureDatetime())
                        .arrivalUtc(flight.getFlightInfo().getSchedule().getArrivalDatetime())
                        .durationMinutes(flight.getFlightInfo().getSchedule().getDurationMinutes())
                        .build())

                .status(FlightDTO.Status.builder()
                        .cancelled(flight.getStats().getIsCancelled() == 1)
                        .diverted(flight.getStats().getIsDiverted() == 1)
                        .delayMinutes(flight.getStats().getTotalDelayMinutes())
                        .build())

                .metrics(FlightDTO.Metrics.builder()
                        .distanceKm(flight.getRoute().getDistanceKm())
                        .speedKmh(flight.getFlightLog().getSpeed())
                        .build())

                .currentPosition(FlightDTO.Position.builder()
                        .longitude(coordinates.get(0))
                        .latitude(coordinates.get(1))
                        .build())

                .build();
    }

    // private AirlineStatDTO convertToDTO()

    // ------------------ METODI --------------------------------

    // ------------------------ GUEST ------------------------

    // 1)
    public List<FlightDTO> searchFlights(String origin, String destination, String dateString) {

        LocalDate date = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);
        Instant startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<FlightMongo> flights = flightRepository.searchFlights(origin, destination, startOfDay, endOfDay);

        return flights.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 2)
    public List<FlightDTO> findLiveFlights() {

        Instant now = getSimulatedNowInstant();
        Instant startWindow = now.minus(24, ChronoUnit.HOURS);
        Instant endWindow = now.plus(12, ChronoUnit.HOURS);
        // check dei voli live tra 24 ore prima e 12 ore dopo per essere sicuri in casi di ritardi / anticipi

        List<FlightMongo> flights = flightRepository.searchFlightsLive(startWindow, endWindow);

        return flights.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    //----------------------------------- TRAFFIC CONTROLLER -----------------------

    // 1)
    public List<AirlineStatDTO> getAirlinesByAvgDelay(TimeInterval range) {
        List<AirlineStatDTO> result = flightRepository.findAirlinesByAvgDelay(
                calculateMinDate(range),
                getSimulatedNowInstant()
        );
        result.forEach(dto -> dto.setScoreType("AVERAGE_DELAY_MINUTES"));
        return result;
    }

    // 2)
    public List<AirlineStatDTO> getAirlinesByTotalFlights(TimeInterval range) {
        List<AirlineStatDTO> result = flightRepository.findAirlinesByTotalFlights(
                calculateMinDate(range),
                getSimulatedNowInstant()
        );
        result.forEach(dto -> dto.setScoreType("TOTAL_FLIGHTS"));
        return result;
    }

    // 3)
    public List<AirlineStatDTO> getAirlinesByTotalDistance(TimeInterval range) {
        List<AirlineStatDTO> result = flightRepository.findAirlinesByTotalDistance(
                calculateMinDate(range),
                getSimulatedNowInstant()
        );
        result.forEach(dto  -> dto.setScoreType("TOTAL_DISTANCE_KM"));
        return result;
    }

    // 7)
    public List<AirlineStatDTO> getAirlinesByAvgRouteDistance(TimeInterval range) {
        List<AirlineStatDTO> result = flightRepository.findAirlinesByAvgRouteDistance(
                calculateMinDate(range),
                getSimulatedNowInstant()
        );
        result.forEach(dto  -> dto.setScoreType("AVG_DISTANCE_KM"));
        return result;
    }

    // 4) TO DO

    // 5)
    public List<AirlineStatDTO> getAirlinesByRoute(String origin, String destination, TimeInterval range) {
        List<AirlineStatDTO> result =  flightRepository.findAirlinesByRoute(
                origin,
                destination,
                calculateMinDate(range),
                getSimulatedNowInstant()
        );
        result.forEach(dto -> dto.setScoreType("FLIGHT_COUNT"));
        return result;
    }

    // 6)
    public List<AirlineStatDTO> getAirlinesByRouteDelay(String origin, String destination, TimeInterval range) {
        List<AirlineStatDTO> result = flightRepository.findAirlinesByRouteDelay(
                origin,
                destination,
                calculateMinDate(range),
                getSimulatedNowInstant()
        );
        result.forEach(dto -> dto.setScoreType("AVG_DELAY_MIN"));
        return result;
    }


    // 8) TO DO

    // 9) TO DO

//--------------------------- AIRLINE REPRESENTATIVE -------------------------------


    public List<TripItineraryDTO> findQuickestRealRoute(String origin, String dest, String dateString, int maxHops) {

        LocalDate date = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);
        Instant tripStartTime = date.atStartOfDay(ZoneOffset.UTC).toInstant();

        System.out.println("DEBUG: Start search " + origin + " -> " + dest + " from " + tripStartTime);

        // 1. Chiamata al Repository (Restituisce List<String> sicura)
        List<String> rawCsvPaths = airportRepository.findCandidatePaths(origin, dest, maxHops);

        System.out.println("DEBUG: Candidati trovati: " + rawCsvPaths.size());

        List<TripItineraryDTO> validItineraries = new ArrayList<>();

        for (String csvPath : rawCsvPaths) {
            // 2. Riconvertiamo la stringa "JFK,LHR,CLT" in Lista ["JFK", "LHR", "CLT"]
            List<String> path = Arrays.asList(csvPath.split(","));

            System.out.println("DEBUG: Processing Path: " + path);

            if (path.size() < 2) continue;

            List<FlightMongo> itineraryFlights = new ArrayList<>();
            Instant currentClock = tripStartTime;
            boolean validPath = true;

            for (int i = 0; i < path.size() - 1; i++) {
                String legOrigin = path.get(i);
                String legDest = path.get(i+1);

                Instant minDeparture = (i == 0) ? currentClock : currentClock.plus(Duration.ofMinutes(45));

                List<FlightMongo> flights = flightRepository.findNextFlight(
                        legOrigin,
                        legDest,
                        minDeparture,
                        PageRequest.of(0, 1)
                );
                FlightMongo flight = flights.isEmpty() ? null : flights.get(0);
                if (flight == null) {
                    System.out.println("   X Volo mancante: " + legOrigin + "->" + legDest);
                    validPath = false;
                    break;
                }

                System.out.println("   V Volo trovato: " + flight.getFlightInfo().getFlightKey());
                itineraryFlights.add(flight);
                currentClock = flight.getFlightInfo().getSchedule().getArrivalDatetime();
            }

            if (validPath && !itineraryFlights.isEmpty()) {
                Instant firstDep = itineraryFlights.get(0).getFlightInfo().getSchedule().getDepartureDatetime();
                Instant lastArr = itineraryFlights.get(itineraryFlights.size()-1).getFlightInfo().getSchedule().getArrivalDatetime();
                double totalMinutes = Duration.between(firstDep, lastArr).toMinutes();

                validItineraries.add(new TripItineraryDTO(
                        totalMinutes,
                        itineraryFlights.size() - 1,
                        itineraryFlights
                ));
            }
        }

        Collections.sort(validItineraries);
        return validItineraries;
    }

    public CityStatsDTO getCityHybridStats(String cityName, TimeInterval range) {
        Instant start = calculateMinDate(range);
        Instant end = getSimulatedNowInstant();

        // Step 1: Neo4j (Trova codici IATA e Nazione)
        List<String> iataCodes = cityRepository.findIataCodesByCity(cityName);
        String country = cityRepository.findCountryByCity(cityName);

        if (iataCodes == null || iataCodes.isEmpty()) {
            // Città non trovata o senza aeroporti
            return new CityStatsDTO(cityName, country != null ? country : "Unknown", 0L, 0L, 0L, 0);
        }

        // Step 2: Mongo (Conta i voli)
        long departures = flightRepository.countDeparturesByAirports(iataCodes, start, end);
        long arrivals = flightRepository.countArrivalsByAirports(iataCodes, start, end);
        long totalFlights = departures + arrivals;

        // Step 3: Assembla il DTO aggiornato
        return new CityStatsDTO(
                cityName,
                country,
                departures,
                arrivals,
                totalFlights,
                iataCodes.size()
        );
    }

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
        List<AirportStatDTO> result =  flightRepository.findAirportsByAvgDelay(
                calculateMinDate(range),
                getSimulatedNowInstant()
        );

        result.forEach(dto  -> dto.setScoreType("AVG_DELAY_MIN"));
        return result;
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
