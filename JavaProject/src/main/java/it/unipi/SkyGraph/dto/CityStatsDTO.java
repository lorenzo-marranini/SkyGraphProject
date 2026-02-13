package com.skygraph.dto;

public interface CityStatsDTO {
    String getCityName();
    String getCountry();
    Long getTotalFlights();   // Somma dei voli di tutti gli aeroporti della città
    Integer getAirportCount(); // Quanti aeroporti ci sono in quella città
}
