package com.team.cops_and_robbers.community.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 국내 벤더를 먼저 호출하고 주소를 얻지 못하면 해외 벤더로 넘긴다.
 * 좌표로 국내·해외를 판정하면 우리가 정한 범위가 벤더의 실제 커버리지와 어긋날 수 있어 응답으로 판정한다.
 */
@Primary
@Component
@RequiredArgsConstructor
public class RoutingGeocodingClient implements GeocodingClient {

    private final VWorldGeocodingClient domesticClient;
    private final GeoapifyGeocodingClient overseasClient;

    @Override
    public GeocodingResult reverseGeocode(Double latitude, Double longitude) {
        GeocodingResult domesticResult = domesticClient.reverseGeocode(latitude, longitude);
        if (domesticResult instanceof GeocodingResult.Resolved) {
            return domesticResult;
        }
        return overseasClient.reverseGeocode(latitude, longitude);
    }

    @Override
    public GeocodingResult findCountry(Double latitude, Double longitude) {
        GeocodingResult domesticResult = domesticClient.findCountry(latitude, longitude);
        if (domesticResult instanceof GeocodingResult.Resolved) {
            return domesticResult;
        }
        return overseasClient.findCountry(latitude, longitude);
    }
}
