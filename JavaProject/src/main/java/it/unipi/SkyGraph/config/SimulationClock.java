package it.unipi.SkyGraph.config;

import it.unipi.SkyGraph.enums.TimeInterval;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Component
public class SimulationClock {

    private static final LocalDate SIMULATED_NOW = LocalDate.of(2026, 2, 25);

    // Definiamo la data di riferimento "ADESSO" statica per la simulazione
    public Instant now() {
        return SIMULATED_NOW.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    /**
     * Calcola la data di inizio (minDate) basata sull'intervallo richiesto
     * rispetto alla data simulata "SIMULATED_NOW".
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
     * Restituisce la fine della giornata "simulata" corrente (25 Feb 2026 23:59:59)
     * Usato come limite superiore (maxDate) per le query.
     */
    public Instant getSimulatedNowInstant() {
        // Prendiamo la fine della giornata corrente o l'inizio del giorno dopo
        return SIMULATED_NOW.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}