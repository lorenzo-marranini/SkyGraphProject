package it.unipi.SkyGraph.config;

import it.unipi.SkyGraph.enums.AirlineSort;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class AirlineSortConverter implements Converter<String, AirlineSort> {
    @Override
    public AirlineSort convert(String source) {
        try {
            return AirlineSort.valueOf(source.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Invalid sort '" + source + "'. Allowed: DELAY, FLIGHTS, TOT_DISTANCE, AVG_DISTANCE"
            );
        }
    }
}