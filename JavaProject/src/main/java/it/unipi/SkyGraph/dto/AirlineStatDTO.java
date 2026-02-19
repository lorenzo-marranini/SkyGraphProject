package it.unipi.SkyGraph.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
public class AirlineStatDTO {
    private String airlineName;
    private Double score;
    private String scoreType;
}