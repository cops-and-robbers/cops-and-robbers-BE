package com.team.cops_and_robbers.community.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.team.cops_and_robbers.common.infrastructure.discord.DiscordNotifier;
import com.team.cops_and_robbers.community.domain.PostAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class GeoapifyGeocodingClient implements GeocodingClient {

    private static final String GEOAPIFY_BASE_URL = "https://api.geoapify.com";
    private static final String REVERSE_GEOCODE_PATH = "/v1/geocode/reverse";
    private static final String ADDRESS_DELIMITER = " ";
    private static final String REGION_DELIMITER = " ";

    /**
     * 국가별로 주소를 어느 언어로 받을지. Geoapify가 주는 country_code가 키다.
     * 앱이 지원하는 건 한국어·일본어·영어 셋인데 lang을 안 보내면 영어로 오므로,
     * 나머지 두 언어만 넣어 두고 여기 없는 국가는 2차 호출 없이 영어를 쓴다.
     */
    private static final Map<String, String> LANGUAGE_BY_COUNTRY = Map.of("jp", "ja", "kr", "ko");

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
        return execute(latitude, longitude, true);
    }

    @Override
    public GeocodingResult findCountry(Double latitude, Double longitude) {
        return execute(latitude, longitude, false);
    }

    private GeocodingResult execute(Double latitude, Double longitude, boolean localized) {
        if (!StringUtils.hasText(apiKey)) {
            log.warn("Geoapify 인증키가 설정되지 않아 해외 역지오코딩을 건너뜁니다.");
            return GeocodingResult.failed();
        }

        try {
            GeoapifyResponse response = requestReverseGeocode(latitude, longitude, null);
            String language = localized ? findLanguage(response) : null;
            if (language != null) {
                response = requestReverseGeocode(latitude, longitude, language);
            }

            PostAddress postAddress = extractPostAddress(response);
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

    /**
     * 기본 응답은 영어라 한국·일본 좌표는 언어를 지정해 한 번 더 부른다.
     * 좌표만으로는 어느 나라인지 알 수 없어 첫 응답의 country_code를 보고 판단한다.
     */
    private GeoapifyResponse requestReverseGeocode(Double latitude, Double longitude, String language) {
        return restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(REVERSE_GEOCODE_PATH)
                            .queryParam("lat", latitude)
                            .queryParam("lon", longitude)
                            .queryParam("format", "json")
                            .queryParam("apiKey", apiKey);
                    if (language != null) {
                        uriBuilder.queryParam("lang", language);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .body(GeoapifyResponse.class);
    }

    private String findLanguage(GeoapifyResponse response) {
        if (response == null || response.results() == null || response.results().isEmpty()) {
            return null;
        }

        String countryCode = response.results().getFirst().countryCode();
        if (!StringUtils.hasText(countryCode)) {
            return null;
        }
        return LANGUAGE_BY_COUNTRY.get(countryCode.toLowerCase(Locale.ROOT));
    }

    /**
     * 해외에는 지번 체계가 없어 전체 주소를 address에, 번지와 도로명을 합쳐 roadAddress에 담는다.
     * <p>
     * 바다처럼 주소가 없는 좌표는 formatted가 비어 있어 이것으로 걸러낸다.
     * country_code가 없는 좌표(공해 경계 등)도 주소 없음으로 본다. 국가를 모르면 어느 목록에도 걸리지 않아
     * 작성자조차 찾을 수 없는 게시글이 된다.
     * name은 좌표에 가장 가까운 장소 이름이라 건물명으로 쓸 수 없다.
     * (타임스퀘어 좌표에서 인근 상점인 "Sunglass Hut"이 나온다.)
     */
    private PostAddress extractPostAddress(GeoapifyResponse response) {
        if (response == null || response.results() == null || response.results().isEmpty()) {
            return PostAddress.empty();
        }

        GeoapifyPlace place = response.results().getFirst();
        if (!StringUtils.hasText(place.formatted()) || !StringUtils.hasText(place.countryCode())) {
            return PostAddress.empty();
        }
        return PostAddress.of(place.formatted(), toRoadAddress(place), null,
                toRegion(place), place.countryCode());
    }

    /**
     * 국내의 시도·시군구·읍면동에 대응하는 3단 표기.
     * county는 city와 값이 겹치는 경우가 많아(무사시노시) 제외하고, 뉴욕처럼 state와 city가 같은 곳은 중복을 제거한다.
     */
    private String toRegion(GeoapifyPlace place) {
        return Stream.of(place.state(), place.city(), place.suburb())
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

    private record GeoapifyPlace(String formatted, String street, String housenumber,
                                 String state, String city, String suburb,
                                 @JsonProperty("country_code") String countryCode) {
    }
}
