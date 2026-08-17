package com.team.cops_and_robbers.community.infrastructure;

import com.team.cops_and_robbers.common.infrastructure.discord.DiscordNotifier;
import com.team.cops_and_robbers.community.domain.PostAddress;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class GeoapifyGeocodingClient implements GeocodingClient {

    private static final String GEOAPIFY_BASE_URL = "https://api.geoapify.com";
    private static final String REVERSE_GEOCODE_PATH = "/v1/geocode/reverse";
    private static final String ADDRESS_DELIMITER = " ";
    private static final String REGION_DELIMITER = " ";

    private final RestClient restClient;
    private final String apiKey;
    private final DiscordNotifier discordNotifier;

    public GeoapifyGeocodingClient(
            RestClient.Builder builder,
            @Value("${geocoding.geoapify.api-key:}") String apiKey,
            DiscordNotifier discordNotifier
    ) {
        this.apiKey = apiKey;
        this.discordNotifier = discordNotifier;
        this.restClient = builder.baseUrl(GEOAPIFY_BASE_URL).build();
    }

    @Override
    public GeocodingResult reverseGeocode(Double latitude, Double longitude) {
        if (!StringUtils.hasText(apiKey)) {
            log.warn("Geoapify 인증키가 설정되지 않아 해외 역지오코딩을 건너뜁니다.");
            return GeocodingResult.failed();
        }

        try {
            PostAddress postAddress = extractPostAddress(requestReverseGeocode(latitude, longitude));
            if (postAddress.isEmpty()) {
                return GeocodingResult.notFound();
            }
            return GeocodingResult.resolved(postAddress);
        } catch (Exception e) {
            log.error("Geoapify 역지오코딩 호출 실패 | lat={}, lng={}", latitude, longitude, e);
            discordNotifier.sendGeocodingFailure(latitude, longitude, e.toString());
            return GeocodingResult.failed();
        }
    }

    private GeoapifyResponse requestReverseGeocode(Double latitude, Double longitude) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder.path(REVERSE_GEOCODE_PATH)
                        .queryParam("lat", latitude)
                        .queryParam("lon", longitude)
                        .queryParam("format", "json")
                        .queryParam("apiKey", apiKey)
                        .build())
                .retrieve()
                .body(GeoapifyResponse.class);
    }

    /**
     * 해외에는 지번 체계가 없어 전체 주소를 address에, 번지와 도로명을 합쳐 roadAddress에 담는다.
     * <p>
     * 바다처럼 주소가 없는 좌표는 formatted가 비어 있고 name에만 지형 이름(예: South Pacific Ocean)이 온다.
     * formatted로 걸러내지 않으면 이런 좌표가 건물명만 있는 주소로 저장된다.
     */
    private PostAddress extractPostAddress(GeoapifyResponse response) {
        if (response == null || response.results() == null || response.results().isEmpty()) {
            return PostAddress.empty();
        }

        GeoapifyPlace place = response.results().getFirst();
        if (!StringUtils.hasText(place.formatted())) {
            return PostAddress.empty();
        }
        return PostAddress.of(place.formatted(), toRoadAddress(place), place.name(), toRegion(place));
    }

    /** 국내의 동 단위에 대응하는 표기. 도쿄처럼 도시와 하위 지역 이름이 같은 경우가 있어 중복을 제거한다. */
    private String toRegion(GeoapifyPlace place) {
        return Stream.of(place.city(), place.suburb())
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.joining(REGION_DELIMITER));
    }

    private String toRoadAddress(GeoapifyPlace place) {
        if (!StringUtils.hasText(place.street())) {
            return null;
        }
        if (!StringUtils.hasText(place.housenumber())) {
            return place.street();
        }
        return place.housenumber() + ADDRESS_DELIMITER + place.street();
    }

    private record GeoapifyResponse(List<GeoapifyPlace> results) {
    }

    private record GeoapifyPlace(String formatted, String name, String street, String housenumber,
                                 String city, String suburb) {
    }
}
