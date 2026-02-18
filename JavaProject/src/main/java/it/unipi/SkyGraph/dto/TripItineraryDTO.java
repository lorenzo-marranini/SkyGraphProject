package it.unipi.SkyGraph.dto;

import it.unipi.SkyGraph.model.FlightMongo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TripItineraryDTO implements Comparable<TripItineraryDTO> {
    private double totalDurationMinutes;
    private int stops;
    private List<FlightMongo> flights; // The sequence of actual flights found

    @Override
    public int compareTo(TripItineraryDTO other) {
        return Double.compare(this.totalDurationMinutes, other.totalDurationMinutes);
    }
}