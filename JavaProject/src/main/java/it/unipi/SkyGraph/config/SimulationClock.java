package it.unipi.SkyGraph.config;

import it.unipi.SkyGraph.enums.TimeInterval;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Component
public class SimulationClock {

    private static final LocalDate SIMULATED_NOW = LocalDate.of(2026, 2, 25);
    private static Instant currentTimeStamp = SIMULATED_NOW.atTime(11, 50).toInstant(ZoneOffset.UTC);


    public Instant now() {
        return currentTimeStamp;
    }

    /**
     * Advances the simulated time by a specified number of minutes.
     */
    public void advanceSimTimeMin(int minutes) {
        currentTimeStamp = currentTimeStamp.plus(minutes, ChronoUnit.MINUTES);
    }

    /**
     * Calculates the start date (minDate) based on the requested interval
     * relative to the simulated date "SIMULATED_NOW".
     */
    public Instant calculateMinDate(TimeInterval range) {
        LocalDate date = switch (range) {
            case LAST_DAY   -> SIMULATED_NOW.minusDays(1);
            case LAST_WEEK  -> SIMULATED_NOW.minusWeeks(1);
            case LAST_MONTH -> SIMULATED_NOW.minusMonths(1);
            case LAST_YEAR  -> SIMULATED_NOW.minusYears(1);
        };
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    /**
     * Returns the end of the current "simulated" day (25 Feb 2026 23:59:59).
     * Used as the upper bound (maxDate) for queries.
     */
    public Instant getSimulatedNowInstant() {
        // Prendiamo la fine della giornata corrente o l'inizio del giorno dopo
        return SIMULATED_NOW.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}