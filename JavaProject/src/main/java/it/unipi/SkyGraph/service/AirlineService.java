package it.unipi.SkyGraph.service;

import it.unipi.SkyGraph.config.SimulationClock;
import it.unipi.SkyGraph.dto.AirlineReportDTO;
import it.unipi.SkyGraph.dto.AirlineStatDTO;
import it.unipi.SkyGraph.enums.TimeInterval;
import it.unipi.SkyGraph.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AirlineService {

    private final FlightRepository flightRepository;
    private final SimulationClock clock;


    // TC1
    public List<AirlineStatDTO> getAirlinesByAvgDelay(TimeInterval range) {
        List<AirlineStatDTO> result = flightRepository.findAirlinesByAvgDelay(
                clock.calculateMinDate(range),
                clock.getSimulatedNowInstant()
        );
        result.forEach(dto -> dto.setScoreType("AVERAGE_DELAY_MINUTES"));
        return result;
    }


    // TC2
    public List<AirlineStatDTO> getAirlinesByTotalFlights(TimeInterval range) {
        List<AirlineStatDTO> result = flightRepository.findAirlinesByTotalFlights(
                clock.calculateMinDate(range),
                clock.getSimulatedNowInstant()
        );
        result.forEach(dto -> dto.setScoreType("TOTAL_FLIGHTS"));
        return result;
    }

    // TC3
    public List<AirlineStatDTO> getAirlinesByTotalDistance(TimeInterval range) {
        List<AirlineStatDTO> result = flightRepository.findAirlinesByTotalDistance(
                clock.calculateMinDate(range),
                clock.getSimulatedNowInstant()
        );
        result.forEach(dto  -> dto.setScoreType("TOTAL_DISTANCE_KM"));
        return result;
    }

    // TC4
    public List<AirlineStatDTO> getAirlinesByAvgRouteDistance(TimeInterval range) {
        List<AirlineStatDTO> result = flightRepository.findAirlinesByAvgRouteDistance(
                clock.calculateMinDate(range),
                clock.getSimulatedNowInstant()
        );
        result.forEach(dto  -> dto.setScoreType("AVG_DISTANCE_KM"));
        return result;
    }

    // TC5
    public List<AirlineStatDTO> getAirlinesByRoute(String origin, String destination, TimeInterval range) {
        List<AirlineStatDTO> result =  flightRepository.findAirlinesByRoute(
                origin,
                destination,
                clock.calculateMinDate(range),
                clock.getSimulatedNowInstant()
        );
        result.forEach(dto -> dto.setScoreType("FLIGHT_COUNT"));
        return result;
    }

    // TC6
    public List<AirlineStatDTO> getAirlinesByRouteDelay(String origin, String destination, TimeInterval range) {
        List<AirlineStatDTO> result = flightRepository.findAirlinesByRouteDelay(
                origin,
                destination,
                clock.calculateMinDate(range),
                clock.getSimulatedNowInstant()
        );
        result.forEach(dto -> dto.setScoreType("AVG_DELAY_MIN"));
        return result;
    }

    // AR10
    public List<AirlineReportDTO> getAirlineReport(TimeInterval range, String AirlineIata) {
        return flightRepository.generateAirlineReport(
                clock.calculateMinDate(range),
                clock.getSimulatedNowInstant(),
                AirlineIata
        );
    }
}
