package com.team.cops_and_robbers.community.post.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 국내 벤더를 먼저 호출하고 주소를 얻지 못하면 해외 벤더로 넘긴다.
 * 좌표로 국내·해외를 판정하면 우리가 정한 범위가 벤더의 실제 커버리지와 어긋날 수 있어 응답으로 판정한다.
 * <p>
 * 다만 국토에서 명백히 벗어난 좌표는 국내 벤더가 답을 줄 수 없으므로 건너뛴다.
 * 국내 벤더는 해외 IP를 막아 dev에서 터널을 경유하는데, 그 첫 연결이 수 초씩 걸려
 * 해외 사용자가 쓰지도 않을 벤더를 기다리고 있었다.
 */
@Primary
@Component
@RequiredArgsConstructor
public class RoutingGeocodingClient implements GeocodingClient {

    private static final double KOREA_MIN_LATITUDE = 33.0;
    private static final double KOREA_MAX_LATITUDE = 39.0;
    private static final double KOREA_MIN_LONGITUDE = 124.0;
    private static final double KOREA_MAX_LONGITUDE = 132.0;

    private final VWorldGeocodingClient domesticClient;
    private final GeoapifyGeocodingClient overseasClient;

    @Override
    public GeocodingResult reverseGeocode(Double latitude, Double longitude) {
        if (isOutsideKorea(latitude, longitude)) {
            return overseasClient.reverseGeocode(latitude, longitude);
        }

        GeocodingResult domesticResult = domesticClient.reverseGeocode(latitude, longitude);
        if (domesticResult instanceof GeocodingResult.Resolved) {
            return domesticResult;
        }
        return overseasClient.reverseGeocode(latitude, longitude);
    }

    @Override
    public GeocodingResult findCountry(Double latitude, Double longitude) {
        if (isOutsideKorea(latitude, longitude)) {
            return overseasClient.findCountry(latitude, longitude);
        }

        GeocodingResult domesticResult = domesticClient.findCountry(latitude, longitude);
        if (domesticResult instanceof GeocodingResult.Resolved) {
            return domesticResult;
        }
        return overseasClient.findCountry(latitude, longitude);
    }

    /**
     * 마라도(33.10), 고성(38.61), 백령도(124.61), 독도(131.87)를 모두 담고 여유를 둔 범위다.
     * 경계가 애매하면 국내 벤더를 먼저 부르는 쪽이 안전하므로 넓게 잡았다.
     */
    private boolean isOutsideKorea(Double latitude, Double longitude) {
        return latitude < KOREA_MIN_LATITUDE || latitude > KOREA_MAX_LATITUDE
                || longitude < KOREA_MIN_LONGITUDE || longitude > KOREA_MAX_LONGITUDE;
    }
}