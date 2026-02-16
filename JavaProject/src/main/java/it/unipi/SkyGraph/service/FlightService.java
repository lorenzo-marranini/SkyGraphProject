package it.unipi.SkyGraph.service;

import it.unipi.SkyGraph.dto.AirlineStatDto;
import it.unipi.SkyGraph.enums.TimeInterval;
import it.unipi.SkyGraph.repository.FlightRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class FlightService {

    @Autowired
    private FlightRepository flightRepository;

    private String calculateMinDate(TimeInterval range) {
        LocalDate now = LocalDate.now();
        LocalDate calculatedDate;

        switch (range) {
            case LAST_DAY:
                calculatedDate = now.minusDays(1);
                break;
            case LAST_WEEK:
                calculatedDate = now.minusWeeks(1);
                break;
            case LAST_MONTH:
                calculatedDate = now.minusMonths(1);
                break;
            case LAST_YEAR:
                calculatedDate = now.minusYears(1);
                break;
            default:
                throw new IllegalArgumentException("Invalid Time Range");
        }

        // Formatta la data come stringa YYYY-MM-DD per MongoDB
        return calculatedDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    
    public List<AirlineStatDto> getAirlinesByAvgDelay(TimeInterval range) {
        String minDate = calculateMinDate(range);
        return flightRepository.findAirlinesByAvgDelay(minDate);
    }


    public List<AirlineStatDto> getAirlinesByTotalFlights(TimeInterval range) {
        String minDate = calculateMinDate(range);
        return flightRepository.findAirlinesByTotalFlights(minDate);
    }


}