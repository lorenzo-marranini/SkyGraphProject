package it.unipi.SkyGraph.dto;

import lombok.Data;
import java.util.List;

@Data
public class QuickestPathDTO {
    private Double totalDuration;
    private List<AirportStep> path;

    @Data
    public static class AirportStep {
        private String iataCode;
        private String name;
    }
}