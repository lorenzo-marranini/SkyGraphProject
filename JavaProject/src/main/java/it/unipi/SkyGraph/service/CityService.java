package it.unipi.SkyGraph.service;

import it.unipi.SkyGraph.config.SimulationClock;
import it.unipi.SkyGraph.dto.*;
import it.unipi.SkyGraph.enums.TimeInterval;
import it.unipi.SkyGraph.model.*;
import it.unipi.SkyGraph.repository.CityRepository;
import it.unipi.SkyGraph.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CityService {

    private final CityRepository cityRepository;
    private final FlightRepository flightRepository;
    private final SimulationClock clock;


    // ----------------------- AIRLINE REPRESENTATIVE -----------------


    // AR7
    public List<CityRankDTO> getMostTraffickedCities(Integer limit) {
        List<CityRankDTO> result = cityRepository.findMostTraffickedCities(limit);
        return result;
    }

    // AR8
    public CityStatsDTO getCityHybridStats(String cityName, TimeInterval range) {
        Instant start = clock.calculateMinDate(range);
        Instant end = clock.getSimulatedNowInstant();

        List<String> iataCodes = cityRepository.findIataCodesByCity(cityName);
        String country = cityRepository.findCountryByCity(cityName);

        if (iataCodes == null || iataCodes.isEmpty()) {
            return new CityStatsDTO(cityName, country != null ? country : "Unknown", 0L, 0L, 0L, 0);
        }

        long departures = flightRepository.countDeparturesByAirports(iataCodes, start, end);
        long arrivals   = flightRepository.countArrivalsByAirports(iataCodes, start, end);

        return new CityStatsDTO(cityName, country, departures, arrivals, departures + arrivals, iataCodes.size());
    }

    public City createOrUpdateCity(City city) {
        return cityRepository.save(city);
    }

    public List<City> getAllCities() {
        return cityRepository.findAll();
    }

    public City getCityById(String cityState) {
        return cityRepository.findById(cityState)
                .orElseThrow(() -> new IllegalArgumentException("City not found"));
    }

    public void deleteCity(String cityState) {
        cityRepository.deleteById(cityState);
    }


}