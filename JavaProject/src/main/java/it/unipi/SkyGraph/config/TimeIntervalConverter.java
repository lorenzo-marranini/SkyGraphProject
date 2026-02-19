package it.unipi.SkyGraph.config;

import it.unipi.SkyGraph.enums.TimeInterval;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class TimeIntervalConverter implements Converter<String, TimeInterval> {
    @Override
    public TimeInterval convert(String source) {
        try {
            return TimeInterval.valueOf(source.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Invalid time range '" + source + "'. Allowed: LAST_DAY, LAST_WEEK, LAST_MONTH, LAST_YEAR"
            );
        }
    }
}