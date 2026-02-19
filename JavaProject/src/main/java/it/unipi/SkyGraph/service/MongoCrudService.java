package it.unipi.SkyGraph.service;

import it.unipi.SkyGraph.model.FlightMongo;
import it.unipi.SkyGraph.repository.AirportRepository;
import it.unipi.SkyGraph.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    public FlightMongo updateFlight(String id, FlightMongo updatedFlight) {
        FlightMongo existing = getFlightById(id);

        // Ensure we don't overwrite the Mongo _id
        updatedFlight.setId(existing.getId());

        // Note: If the origin/destination or duration changes during an update,
        // you would ideally need complex graph logic here to decrement the old route's
        // num_voli and increment the new one. For simplicity, we just save the document.
        return flightRepository.save(updatedFlight);
    }

    public void deleteFlight(String id) {
        flightRepository.deleteById(id);
    }
}