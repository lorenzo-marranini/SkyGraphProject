package it.unipi.SkyGraph.service;

import it.unipi.SkyGraph.dto.*;
import it.unipi.SkyGraph.model.*;
import it.unipi.SkyGraph.repository.AirportRepository;
import it.unipi.SkyGraph.repository.CityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CityService {

    private final CityRepository cityRepository;


    // ----------------------- AIRLINE REPRESENTATIVE -----------------
    // 3)
    public List<CityRankDTO> getMostTraffickedCities() {
        List<CityRankDTO> result = cityRepository.findMostTraffickedCities();
        return result;
    }




}