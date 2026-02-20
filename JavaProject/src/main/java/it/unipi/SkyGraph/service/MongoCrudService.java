package it.unipi.SkyGraph.service;

import it.unipi.SkyGraph.model.FlightMongo;
import it.unipi.SkyGraph.repository.AirportRepository;
import it.unipi.SkyGraph.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MongoCrudService {

    private final FlightRepository flightRepository;
    private final AirportRepository airportRepository;

    public FlightMongo createFlight(FlightMongo flight) {
        // 1. Save the flight document to MongoDB
        FlightMongo savedFlight = flightRepository.save(flight);

        // 2. Extract routing info to sync with Neo4j
        String origin = flight.getRoute().getOrigin().getIata();
        String dest = flight.getRoute().getDestination().getIata();

        // Extract the scheduled duration from the Mongo document
        double duration = flight.getFlightInfo().getSchedule().getDurationMinutes();

        // 3. Trigger the UPSERT in Neo4j to update the ROUTE relationship
        airportRepository.upsertRouteRelationship(origin, dest, duration);

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
}