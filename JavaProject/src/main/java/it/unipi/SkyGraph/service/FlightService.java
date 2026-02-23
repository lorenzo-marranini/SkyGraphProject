package it.unipi.SkyGraph.service;

import it.unipi.SkyGraph.config.SimulationClock;
import it.unipi.SkyGraph.dto.*;
import it.unipi.SkyGraph.enums.TimeInterval;
import it.unipi.SkyGraph.model.AirportMongo;
import it.unipi.SkyGraph.model.FlightMongo;
import it.unipi.SkyGraph.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlightService {

    private final FlightRepository flightRepository;
    private final AirportRepository airportRepository;
    private final SimulationClock clock;
    private final AirportMongoRepository airportMongoRepository;

    /**
     * Transforms a FlightMongo Obj in a FlightDTO
     */
    private FlightDTO convertToDTO(FlightMongo flight) {

        Double longitude = null;
        Double latitude = null;
        Integer speedKmh = null;

        // Check for flightlog
        if (flight.getFlightLog() != null) {
            speedKmh = flight.getFlightLog().getSpeed();


            if (flight.getFlightLog().getLocation() != null &&
                    flight.getFlightLog().getLocation().getCoordinates() != null &&
                    flight.getFlightLog().getLocation().getCoordinates().size() >= 2) {

                var coordinates = flight.getFlightLog().getLocation().getCoordinates();
                longitude = coordinates.get(0);
                latitude = coordinates.get(1);
            }
        }

        boolean isCancelled = flight.getStats() != null && flight.getStats().getIsCancelled() != null && flight.getStats().getIsCancelled() == 1;
        boolean isDiverted = flight.getStats() != null && flight.getStats().getIsDiverted() != null && flight.getStats().getIsDiverted() == 1;
        Integer delay = flight.getStats() != null ? flight.getStats().getTotalDelayMinutes() : null;

        //build the DTO
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
                        .build())

                .status(FlightDTO.Status.builder()
                        .cancelled(isCancelled)
                        .diverted(isDiverted)
                        .delayMinutes(delay)
                        .build())

                .metrics(FlightDTO.Metrics.builder()
                        .distanceKm(flight.getRoute().getDistanceKm())
                        .speedKmh(speedKmh)
                        .build())


                .currentPosition(FlightDTO.Position.builder()
                        .longitude(longitude)
                        .latitude(latitude)
                        .build())

                .build();
    }

    // ------------------------ GUEST ------------------------
    // G1
    public List<FlightDTO> getFlights(String origin, String destination, String dateString) {

        LocalDate date = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);
        Instant startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<FlightMongo> flights = flightRepository.searchFlights(origin, destination, startOfDay, endOfDay);

        if( flights.isEmpty()){ System.out.println("No flights found"); }

        return flights.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // G2
    public Page<FlightDTO> findLiveFlights(Pageable pageable) {

        Instant now = clock.getSimulatedNowInstant();
        Instant startWindow = now.minus(24, ChronoUnit.HOURS);
        Instant endWindow = now.plus(12, ChronoUnit.HOURS);
        // We check flights 24 hours before and 12 hours after to be sure to get a satisfactory number

        Page<FlightMongo> flights = flightRepository.searchPagedFlightsLive(startWindow, endWindow, pageable);

        return flights.map(this::convertToDTO);
    }

    // --------------------- AIRLINE REPRESENTATIVE -------------------

    // AR1
    /**
    * Finds the quickest real itinerary from {@code origin} to {@code dest} on the given date,
    * allowing up to {@code maxHops} intermediate stops. Candidate paths are retrieved from Neo4j;
    * for each path, flights are chained ensuring a minimum 45-minute connection time.
    * Returns the itinerary with the shortest total duration, or {@code null} if none is feasible.
    */
    public TripItineraryDTO findQuickestRealRoute(String origin, String dest, String dateString, int maxHops) {

        LocalDate date = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);
        Instant tripStartTime = date.atStartOfDay(ZoneOffset.UTC).toInstant();


        List<String> rawCsvPaths = airportRepository.findCandidatePaths(origin, dest, maxHops);

        List<TripItineraryDTO> validItineraries = new ArrayList<>();

        for (String csvPath : rawCsvPaths) {
            // the query returns a string, which we convert to list
            List<String> path = Arrays.asList(csvPath.split(","));
            //if there is only one path, no need to check them all
            if (path.size() < 2) continue;

            List<FlightMongo> itineraryFlights = new ArrayList<>();
            Instant currentClock = tripStartTime;
            boolean validPath = true;
            //iterate over all candidate paths
            for (int i = 0; i < path.size() - 1; i++) {
                String legOrigin = path.get(i);
                String legDest = path.get(i+1);

                Instant minDeparture = (i == 0) ? currentClock : currentClock.plus(Duration.ofMinutes(45));
                //check for existance of suitable flight
                List<FlightMongo> flights = flightRepository.findNextFlight(
                        legOrigin,
                        legDest,
                        minDeparture,
                        PageRequest.of(0, 1)
                );

                FlightMongo flight = flights.isEmpty() ? null : flights.get(0);
                if (flight == null) {
                    validPath = false;
                    break;
                }

                itineraryFlights.add(flight);
                currentClock = flight.getFlightInfo().getSchedule().getArrivalDatetime();
            }
            //check for path duration
            if (validPath && !itineraryFlights.isEmpty()) {
                Instant firstDep = itineraryFlights.get(0).getFlightInfo().getSchedule().getDepartureDatetime();
                Instant lastArr = itineraryFlights.get(itineraryFlights.size()-1).getFlightInfo().getSchedule().getArrivalDatetime();
                double totalMinutes = Duration.between(firstDep, lastArr).toMinutes();
                //add it to list
                validItineraries.add(new TripItineraryDTO(
                        totalMinutes,
                        itineraryFlights.size() - 1,
                        itineraryFlights
                ));
            }
        }
        // return minimum value of validItineraries
        return validItineraries.stream()
                .min(Comparator.comparingDouble(TripItineraryDTO::getTotalDurationMinutes))
                .orElse(null);
    }


    // AR2
    public List<RouteStatsDTO> getRoutesByFlightCount(TimeInterval range, Integer limit) {
        return flightRepository.findRoutesByFlightCount(
                clock.calculateMinDate(range),
                clock.getSimulatedNowInstant(),
                limit
        );
    }

    // AR2.1
    public List<RouteStatsDTO> getRoutesByCancelledCount(TimeInterval range, Integer limit) {
        return flightRepository.findRoutesByCancelledCount(
                clock.calculateMinDate(range),
                clock.getSimulatedNowInstant(),
                limit
        );
    }

    // AR2.2
    public List<RouteStatsDTO> getRoutesByDivertedCount(TimeInterval range, Integer limit) {
        return flightRepository.findRoutesByDivertedCount(
                clock.calculateMinDate(range),
                clock.getSimulatedNowInstant(),
                limit
        );
    }

    // AR9
    public List<DayStatsDTO> getDaysByAvgDelay(TimeInterval range) {
        return flightRepository.findDaysByAvgDelay(
                clock.calculateMinDate(range),
                clock.getSimulatedNowInstant()
        );
    }

    // --- Updates ---
    public void updateFlightLog(FlightLogDTO dto) {
        FlightMongo.FlightLog log = new FlightMongo.FlightLog(
                new FlightMongo.GeoLocation("Point", List.of(dto.getLon(), dto.getLat())),
                dto.getAlt(),
                dto.getGspeed(),
                dto.getTimestamp(),
                dto.getEta()
        );
        flightRepository.updateFlightLogByKey(dto.getFlightKey(), log);
    }

    /**
    * Scheduled task (every 5 minutes) that detects landing flights.
    * A flight is considered landed when its altitude is below 500 ft and the current
    * simulated time has passed its ETA. On landing, the actual delay is computed and
    * persisted via {@link FlightRepository#finalizeFlight}.
    */
    @Scheduled(fixedRate = 300000)
    public void checkLandingFlights() {
        Instant now = clock.now();
        Instant startOfDay = now.truncatedTo(ChronoUnit.DAYS);
        Instant endOfDay = startOfDay.plus(1, ChronoUnit.DAYS);

        List<FlightMongo> activeFlights = flightRepository.getAllFlightsLive(startOfDay, endOfDay);;

        for (FlightMongo flight : activeFlights) {
            FlightMongo.FlightLog log = flight.getFlightLog();

            if (log == null || log.getEta() == null) continue;


            boolean isLowAltitude = log.getAltitude() < 500;

            // Check if actual time reached the eta of flightlog
            boolean isTimeReached = now.isAfter(log.getEta()) || now.equals(log.getEta());

            if (isLowAltitude && isTimeReached) {
                //compute actual delay
                long delayMinutes = ChronoUnit.MINUTES.between(
                        flight.getFlightInfo().getSchedule().getArrivalDatetime(),
                        log.getEta()
                );


                long finalDelay = Math.max(0, delayMinutes);

                flightRepository.finalizeFlight(flight.getFlightInfo().getFlightKey(), finalDelay);
            }
        }
    }

    /**
     * Creates a new flight from the given DTO. Resolves origin and destination airports,
     * computes the Haversine distance, generates a unique flight key, and persists the entity.
     * The corresponding Neo4j route relationship is upserted first; if MongoDB insertion fails,
     * a manual Neo4j rollback is required (logged as an error).
     *
     * @throws IllegalArgumentException if either airport IATA code is not found
     * @throws RuntimeException if Neo4j or MongoDB persistence fails
    */
    public FlightMongo createFlight(FlightCreateDTO dto) {

        AirportMongo origin = airportMongoRepository.findById(dto.getOriginIata())
                .orElseThrow(() -> new IllegalArgumentException("Origin airport not found in DB: " + dto.getOriginIata()));

        AirportMongo destination = airportMongoRepository.findById(dto.getDestinationIata())
                .orElseThrow(() -> new IllegalArgumentException("Destination airport not found in DB: " + dto.getDestinationIata()));

        FlightMongo.Airline airline = new FlightMongo.Airline(dto.getAirlineIata(), dto.getAirlineName());
        FlightMongo.Schedule schedule = new FlightMongo.Schedule(dto.getDepartureDatetime(), dto.getArrivalDatetime());

        //flight_key generation
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_M_d_HHmm").withZone(ZoneId.of("UTC"));
        String flightKey = String.format("%s_%s_%s_%s",
                dto.getOriginIata(), dto.getDestinationIata(), dto.getAirlineIata(), formatter.format(dto.getDepartureDatetime()));

        FlightMongo.FlightInfo flightInfo = new FlightMongo.FlightInfo(flightKey, airline, schedule);


        double originLon = origin.getLocation().getCoordinates().get(0);
        double originLat = origin.getLocation().getCoordinates().get(1);

        double destLon = destination.getLocation().getCoordinates().get(0);
        double destLat = destination.getLocation().getCoordinates().get(1);

        // Calcola la distanza in chilometri
        double calculatedDistanceKm = calculateHaversineDistance(originLat, originLon, destLat, destLon);

        // Costruisci gli AirportDetails
        FlightMongo.AirportDetails originDetails = new FlightMongo.AirportDetails(
                origin.getId(), origin.getName(), origin.getCity(), origin.getState(), origin.getCountry()
        );
        FlightMongo.AirportDetails destDetails = new FlightMongo.AirportDetails(
                destination.getId(), destination.getName(), destination.getCity(), destination.getState(), destination.getCountry()
        );

        // Passa la distanza calcolata al posto del DTO
        FlightMongo.Route route = new FlightMongo.Route(originDetails, destDetails, calculatedDistanceKm);
        FlightMongo.Stats stats = new FlightMongo.Stats(
                dto.getTotDelayMinutes(), dto.getIsCancelled(), dto.getIsDiverted(), dto.getAirTimeMinutes()
        );

        FlightMongo flightToSave = new FlightMongo();
        flightToSave.setFlightInfo(flightInfo);
        flightToSave.setRoute(route);
        flightToSave.setStats(stats);

        try {
            airportRepository.upsertRouteRelationship(dto.getOriginIata(), dto.getDestinationIata(), dto.getDurationMinutes().doubleValue());
        } catch (Exception e) {
            log.error("Neo4j creation failed for flightKey: {}. MongoDB insertion aborted.", flightKey, e);
            throw new RuntimeException("Neo4j Creation Failed: " + e.getMessage(), e);
        }

        try {
            return flightRepository.save(flightToSave);
        } catch (Exception e) {
            log.error("POSSIBLE NEO4J INCONSISTENCY. Manual rollback needed for flightKey: {}. " +
                            "Action required: DECREMENT (remove) route {}->{} with duration {}.",
                    flightKey, dto.getOriginIata(), dto.getDestinationIata(), dto.getDurationMinutes().doubleValue(), e);

            throw new RuntimeException("MongoDB Insertion Failed resulting in possible Neo4j inconsistency. Check system logs.", e);
        }
    }

    /**
     * Updates an existing flight identified by {@code flightKey}. If the route or duration changes,
     * the old Neo4j relationship is decremented and the new one is upserted before applying
     * MongoDB changes. If MongoDB save fails after the Neo4j update, a manual rollback is required.
     *
     * @throws IllegalArgumentException if the flight or either airport is not found
     * @throws RuntimeException if Neo4j or MongoDB persistence fails
     */
    public FlightMongo updateFlight(String flightKey, FlightCreateDTO dto) {
        // 1. Recupera il volo esistente da MongoDB prima della modifica
        FlightMongo existing = flightRepository.findByFlightKey(flightKey)
                .orElseThrow(() -> new IllegalArgumentException("Flight not found with key: " + flightKey));

        String oldOrigin = existing.getRoute().getOrigin().getIata();
        String oldDest = existing.getRoute().getDestination().getIata();
        double oldDuration = Duration.between(
                existing.getFlightInfo().getSchedule().getDepartureDatetime(),
                existing.getFlightInfo().getSchedule().getArrivalDatetime()
        ).toMinutes();

        String newOrigin = dto.getOriginIata();
        String newDest = dto.getDestinationIata();
        double newDuration = Duration.between(
                dto.getDepartureDatetime(),
                dto.getArrivalDatetime()
        ).toMinutes();

        boolean routeChanged = !oldOrigin.equals(newOrigin) || !oldDest.equals(newDest);
        boolean durationChanged = oldDuration != newDuration;
        try {
            if (routeChanged || durationChanged) {
                airportRepository.decrementRouteRelationship(oldOrigin, oldDest, oldDuration);
                airportRepository.upsertRouteRelationship(newOrigin, newDest, newDuration);
            }
        } catch (Exception e) {
            log.error("Neo4j update failed for flightKey: {}. MongoDB updates aborted.", flightKey, e);
            throw new RuntimeException("Neo4j Update Failed: " + e.getMessage(), e);
        }

        AirportMongo originAirport = airportMongoRepository.findById(newOrigin)
                .orElseThrow(() -> new IllegalArgumentException("Origin airport not found: " + newOrigin));
        AirportMongo destAirport = airportMongoRepository.findById(newDest)
                .orElseThrow(() -> new IllegalArgumentException("Destination airport not found: " + newDest));

        // 6. Ricalcola la flightKey e la distanza con i nuovi dati
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_M_d_HHmm").withZone(ZoneId.of("UTC"));
        String newFlightKey = String.format("%s_%s_%s_%s",
                newOrigin, newDest, dto.getAirlineIata(), formatter.format(dto.getDepartureDatetime()));

        double originLon = originAirport.getLocation().getCoordinates().get(0);
        double originLat = originAirport.getLocation().getCoordinates().get(1);
        double destLon = destAirport.getLocation().getCoordinates().get(0);
        double destLat = destAirport.getLocation().getCoordinates().get(1);
        double newDistanceKm = calculateHaversineDistance(originLat, originLon, destLat, destLon);


        existing.getFlightInfo().setFlightKey(newFlightKey);
        existing.getFlightInfo().getAirline().setIata(dto.getAirlineIata());
        existing.getFlightInfo().getAirline().setName(dto.getAirlineName());
        existing.getFlightInfo().getSchedule().setDepartureDatetime(dto.getDepartureDatetime());
        existing.getFlightInfo().getSchedule().setArrivalDatetime(dto.getArrivalDatetime());

        FlightMongo.AirportDetails originDetails = new FlightMongo.AirportDetails(
                originAirport.getId(), originAirport.getName(), originAirport.getCity(), originAirport.getState(), originAirport.getCountry()
        );
        FlightMongo.AirportDetails destDetails = new FlightMongo.AirportDetails(
                destAirport.getId(), destAirport.getName(), destAirport.getCity(), destAirport.getState(), destAirport.getCountry()
        );

        existing.getRoute().setOrigin(originDetails);
        existing.getRoute().setDestination(destDetails);
        existing.getRoute().setDistanceKm(newDistanceKm);

        existing.getStats().setTotalDelayMinutes(dto.getTotDelayMinutes());
        existing.getStats().setIsCancelled(dto.getIsCancelled());
        existing.getStats().setIsDiverted(dto.getIsDiverted());
        existing.getStats().setAirTimeMinutes(dto.getAirTimeMinutes());
        try {
        return flightRepository.save(existing);
        } catch (Exception e) {
            log.error("POSSIBLE NEO4J INCONSISTENCY. Manual rollback needed for flightKey: {}. " +
                            "Old Route: {}->{} (duration: {}), New Route: {}->{} (duration: {}).",
                    flightKey, oldOrigin, oldDest, oldDuration, newOrigin, newDest, newDuration, e);

            throw new RuntimeException("MongoDB Update Failed resulting in possible Neo4j inconsistency. Check system logs.", e);
        }
    }

    /**
     * Deletes the flight identified by {@code flightKey} and decrements the corresponding
     * Neo4j route relationship. If MongoDB deletion fails after the Neo4j update,
     * a manual rollback (route increment) is required.
     *
     * @throws IllegalArgumentException if no flight is found with the given key
     * @throws RuntimeException if Neo4j or MongoDB operation fails
     */
    public FlightDTO deleteFlight(String flightKey) {
        FlightMongo existing = flightRepository.findByFlightKey(flightKey)
                .orElseThrow(() -> new IllegalArgumentException("Flight not found with key: " + flightKey));

        //graph update
        String origin = existing.getRoute().getOrigin().getIata();
        String dest = existing.getRoute().getDestination().getIata();
        double duration = Duration.between(
                existing.getFlightInfo().getSchedule().getDepartureDatetime(),
                existing.getFlightInfo().getSchedule().getArrivalDatetime()
        ).toMinutes();
        try {
            airportRepository.decrementRouteRelationship(origin, dest, duration);
        } catch (Exception e) {
            log.error("Neo4j update failed during deletion of flightKey: {}. MongoDB deletion aborted.", flightKey, e);
            throw new RuntimeException("Neo4j Deletion Update Failed: " + e.getMessage(), e);
        }

        try {
            flightRepository.delete(existing);
        } catch (Exception e) {
            log.error("POSSIBLE NEO4J INCONSISTENCY. Manual rollback needed for flightKey: {}. " +
                            "Action required: RESTORE (increment) route {}->{} with duration {}.",
                    flightKey, origin, dest, duration, e);

            throw new RuntimeException("MongoDB Deletion Failed resulting in possible Neo4j inconsistency. Check system logs.", e);
        }
        return convertToDTO(existing);
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; //Earth radius (km)

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return Math.round((R * c) * 100.0) / 100.0;
    }
}
