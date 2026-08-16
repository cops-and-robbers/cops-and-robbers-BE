package com.team.cops_and_robbers.community.infrastructure;

public interface GeocodingClient {

    GeocodingResult reverseGeocode(Double latitude, Double longitude);
}
