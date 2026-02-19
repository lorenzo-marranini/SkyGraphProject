package it.unipi.SkyGraph.config;

import it.unipi.SkyGraph.enums.AirportSort;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class AirportSortConverter implements Converter<String, AirportSort> {
    @Override
    public AirportSort convert(String source) {
        try {
            return AirportSort.valueOf(source.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Invalid sort '" + source + "'. Allowed: DELAY"
            );
        }
    }
}
