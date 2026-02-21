package it.unipi.SkyGraph.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@AllArgsConstructor
@Data
public class FlightLogDTO implements Comparable<FlightLogDTO> {

    private String flight;
    private double lat;
    private double lon;
    private int alt;
    private int gspeed;
    private int vspeed;
    private Instant timestamp;
    private String origIata;
    private String destIata;
    private Instant eta;
    private String flightKey;

    @Override
    public int compareTo(FlightLogDTO other) {
        return this.timestamp.compareTo(other.timestamp);
    }

}
