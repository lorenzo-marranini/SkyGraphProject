package it.unipi.SkyGraph.service;

import it.unipi.SkyGraph.config.SimulationClock;
import it.unipi.SkyGraph.dto.*;
import it.unipi.SkyGraph.enums.TimeInterval;
import it.unipi.SkyGraph.model.AirportMongo;
import it.unipi.SkyGraph.model.FlightMongo;
import it.unipi.SkyGraph.repository.AirportMongoRepository;
import it.unipi.SkyGraph.repository.AirportRepository;
import it.unipi.SkyGraph.repository.CityRepository;
import it.unipi.SkyGraph.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
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
    private final SimulationClock clock;
    private final AirportMongoRepository airportMongoRepository;
    /**
     * Transforms a FlightMongo Obj in a FlightDTO
     */
    private FlightDTO convertToDTO(FlightMongo flight) {

        // 1. Inizializziamo a null le variabili opzionali
        Double longitude = null;
        Double latitude = null;
        Integer speedKmh = null;

        // 2. Controlliamo se il flightLog esiste prima di estrarre i dati
        if (flight.getFlightLog() != null) {
            speedKmh = flight.getFlightLog().getSpeed();

            // Controlliamo che esista la location e l'array di coordinate
            if (flight.getFlightLog().getLocation() != null &&
                    flight.getFlightLog().getLocation().getCoordinates() != null &&
                    flight.getFlightLog().getLocation().getCoordinates().size() >= 2) {

                var coordinates = flight.getFlightLog().getLocation().getCoordinates();
                longitude = coordinates.get(0);
                latitude = coordinates.get(1);
            }
        }

        // 3. Estraiamo in sicurezza anche le statistiche per evitare crash sui Wrapper Integer
        boolean isCancelled = flight.getStats() != null && flight.getStats().getIsCancelled() != null && flight.getStats().getIsCancelled() == 1;
        boolean isDiverted = flight.getStats() != null && flight.getStats().getIsDiverted() != null && flight.getStats().getIsDiverted() == 1;
        Integer delay = flight.getStats() != null ? flight.getStats().getTotalDelayMinutes() : null;

        // 4. Costruiamo il DTO
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
                        .cancelled(isCancelled)
                        .diverted(isDiverted)
                        .delayMinutes(delay)
                        .build())

                .metrics(FlightDTO.Metrics.builder()
                        .distanceKm(flight.getRoute().getDistanceKm())
                        .speedKmh(speedKmh) // Ora è sicuro (o numero o null)
                        .build())

                // Se non ci sono coordinate, costruiamo l'oggetto con campi null, oppure potresti non settarlo affatto
                .currentPosition(FlightDTO.Position.builder()
                        .longitude(longitude)
                        .latitude(latitude)
                        .build())

                .build();
    }
    // private AirlineStatDTO convertToDTO()

    // ------------------ METODI --------------------------------

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
    public List<FlightDTO> findLiveFlights() {

        Instant now = clock.getSimulatedNowInstant();
        Instant startWindow = now.minus(24, ChronoUnit.HOURS);
        Instant endWindow = now.plus(12, ChronoUnit.HOURS);
        // check dei voli live tra 24 ore prima e 12 ore dopo per essere sicuri in casi di ritardi / anticipi

        List<FlightMongo> flights = flightRepository.searchFlightsLive(startWindow, endWindow);

        return flights.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // --------------------- AIRLINE REPRESENTATIVE -------------------

    // AR1

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


    // AR2
    public List<RouteStatsDTO> getRoutesByFlightCount(TimeInterval range) {
        return flightRepository.findRoutesByFlightCount(
                clock.calculateMinDate(range),
                clock.getSimulatedNowInstant()
        );
    }

    // AR2.1
    public List<RouteStatsDTO> getRoutesByCancelledCount(TimeInterval range) {
        return flightRepository.findRoutesByCancelledCount(
                clock.calculateMinDate(range),
                clock.getSimulatedNowInstant()
        );
    }

    // AR2.2
    public List<RouteStatsDTO> getRoutesByDivertedCount(TimeInterval range) {
        return flightRepository.findRoutesByDivertedCount(
                clock.calculateMinDate(range),
                clock.getSimulatedNowInstant()
        );
    }

    // AR9
    public List<DayStatsDTO> getDaysByAvgDelay(TimeInterval range) {
        return flightRepository.findDaysByAvgDelay(
                clock.calculateMinDate(range),
                clock.getSimulatedNowInstant()
        );
    }

    // --- ALTRI METODI (Updates) ---

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

    @Scheduled(fixedRate = 300000)
    public void checkLandingFlights() {
        Instant now = clock.now();
        Instant startOfDay = now.truncatedTo(ChronoUnit.DAYS);
        Instant endOfDay = startOfDay.plus(1, ChronoUnit.DAYS);

        List<FlightMongo> activeFlights = flightRepository.searchFlightsLive(startOfDay, endOfDay);

        for (FlightMongo flight : activeFlights) {
            FlightMongo.FlightLog log = flight.getFlightLog();

            if (log == null || log.getEta() == null) continue;

            // 1. Verifica Altitudine Bassa (es. sotto i 500 piedi)
            boolean isLowAltitude = log.getAltitude() < 500;

            // 2. Verifica se il tempo attuale ha raggiunto o superato l'ETA
            boolean isTimeReached = now.isAfter(log.getEta()) || now.equals(log.getEta());

            if (isLowAltitude && isTimeReached) {
                // Calcolo del ritardo effettivo: ETA (reale) - Departure + Duration (previsto)
                // Oppure più semplicemente: ETA - Arrival_Datetime previsto
                long delayMinutes = ChronoUnit.MINUTES.between(
                        flight.getFlightInfo().getSchedule().getArrivalDatetime(),
                        log.getEta()
                );

                // Se il volo è arrivato in anticipo, il delay potrebbe essere negativo.
                // Di solito si normalizza a 0 se non si tracciano gli anticipi.
                long finalDelay = Math.max(0, delayMinutes);

                // Esegue l'aggiornamento e pulisce il log
                flightRepository.finalizeFlight(flight.getFlightInfo().getFlightKey(), finalDelay);
            }
        }
    }

    @Transactional
    public FlightMongo createFlight(FlightCreateDTO dto) {

        AirportMongo origin = airportMongoRepository.findById(dto.getOriginIata())
                .orElseThrow(() -> new IllegalArgumentException("Origin airport not found in DB: " + dto.getOriginIata()));

        AirportMongo destination = airportMongoRepository.findById(dto.getDestinationIata())
                .orElseThrow(() -> new IllegalArgumentException("Destination airport not found in DB: " + dto.getDestinationIata()));

        FlightMongo.Airline airline = new FlightMongo.Airline(dto.getAirlineIata(), dto.getAirlineName());
        FlightMongo.Schedule schedule = new FlightMongo.Schedule(dto.getDurationMinutes(), dto.getDepartureDatetime(), dto.getArrivalDatetime());

        //generazione chiave di volo
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_M_d_HHmm").withZone(ZoneId.of("UTC"));
        String flightKey = String.format("%s_%s_%s_%s",
                dto.getOriginIata(), dto.getDestinationIata(), dto.getAirlineIata(), formatter.format(dto.getDepartureDatetime()));

        FlightMongo.FlightInfo flightInfo = new FlightMongo.FlightInfo(flightKey, airline, schedule);

        // TODO: togliere queste dopo rimozione coordinate
        FlightMongo.GeoLocation originGeo = new FlightMongo.GeoLocation(
                origin.getLocation().getType(),
                origin.getLocation().getCoordinates()
        );
        FlightMongo.GeoLocation destGeo = new FlightMongo.GeoLocation(
                destination.getLocation().getType(),
                destination.getLocation().getCoordinates()
        );
        double originLon = origin.getLocation().getCoordinates().get(0);
        double originLat = origin.getLocation().getCoordinates().get(1);

        double destLon = destination.getLocation().getCoordinates().get(0);
        double destLat = destination.getLocation().getCoordinates().get(1);

        // Calcola la distanza in chilometri
        double calculatedDistanceKm = calculateHaversineDistance(originLat, originLon, destLat, destLon);

        // Costruisci gli AirportDetails
        FlightMongo.AirportDetails originDetails = new FlightMongo.AirportDetails(
                origin.getId(), origin.getName(), origin.getCity(), origin.getState(), origin.getCountry(), originGeo
        );
        FlightMongo.AirportDetails destDetails = new FlightMongo.AirportDetails(
                destination.getId(), destination.getName(), destination.getCity(), destination.getState(), destination.getCountry(), destGeo
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

        FlightMongo savedFlight = flightRepository.save(flightToSave);

        airportRepository.upsertRouteRelationship(dto.getOriginIata(), dto.getDestinationIata(), dto.getDurationMinutes().doubleValue());

        return savedFlight;
    }
    public FlightMongo getFlightById(String id) {
        return flightRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Flight not found in MongoDB"));
    }

    public List<FlightMongo> getAllFlights() {
        // Caution: In a real app, you'd want to paginate this!
        return flightRepository.findAll();
    }

    @Transactional
    public FlightMongo updateFlight(String id, FlightMongo updatedFlight) {
        // 1. Recupera il volo esistente da MongoDB prima della modifica
        FlightMongo existing = getFlightById(id);

        // Estrai le vecchie informazioni di rotta e durata
        String oldOrigin = existing.getRoute().getOrigin().getIata();
        String oldDest = existing.getRoute().getDestination().getIata();
        double oldDuration = existing.getFlightInfo().getSchedule().getDurationMinutes();

        // Estrai le nuove informazioni (che potrebbero essere state modificate dall'utente)
        String newOrigin = updatedFlight.getRoute().getOrigin().getIata();
        String newDest = updatedFlight.getRoute().getDestination().getIata();
        double newDuration = updatedFlight.getFlightInfo().getSchedule().getDurationMinutes();

        // Controlla se qualcosa che impatta Neo4j è effettivamente cambiato
        boolean routeChanged = !oldOrigin.equals(newOrigin) || !oldDest.equals(newDest);
        boolean durationChanged = oldDuration != newDuration;

        if (routeChanged || durationChanged) {
            airportRepository.decrementRouteRelationship(oldOrigin, oldDest, oldDuration);

            airportRepository.upsertRouteRelationship(newOrigin, newDest, newDuration);
        }

        // 3. Salva l'aggiornamento su MongoDB
        updatedFlight.setId(existing.getId());
        return flightRepository.save(updatedFlight);
    }


    public void deleteFlight(String id) {
        FlightMongo existing = getFlightById(id);

        String origin = existing.getRoute().getOrigin().getIata();
        String dest = existing.getRoute().getDestination().getIata();
        double duration = existing.getFlightInfo().getSchedule().getDurationMinutes();

        airportRepository.decrementRouteRelationship(origin, dest, duration);

        flightRepository.deleteById(id);
    }


    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Raggio medio della Terra in chilometri

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // Arrotondiamo a 2 cifre decimali per pulizia, oppure restituisci direttamente (R * c)
        return Math.round((R * c) * 100.0) / 100.0;
    }
}
