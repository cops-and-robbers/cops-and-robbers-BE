package com.team.cops_and_robbers.community.infrastructure;

import com.team.cops_and_robbers.common.infrastructure.discord.DiscordNotifier;
import com.team.cops_and_robbers.community.domain.PostAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class VWorldGeocodingClient implements GeocodingClient {

    private static final String VWORLD_BASE_URL = "https://api.vworld.kr";
    private static final String GET_ADDRESS_PATH = "/req/address";
    private static final String STATUS_OK = "OK";
    private static final String STATUS_NOT_FOUND = "NOT_FOUND";
    private static final String TYPE_PARCEL = "parcel";
    private static final String TYPE_ROAD = "road";

    /** 도로명 응답은 {@code 서울특별시 송파구 올림픽로 300 (신천동,롯데월드타워)} 형태로, 괄호 안에 법정동과 건물명이 함께 온다. */
    private static final Pattern ROAD_TEXT = Pattern.compile("^(.*?)\\s*\\((.*)\\)$");
    private static final String BUILDING_NAME_DELIMITER = ",";
    private static final int BUILDING_NAME_INDEX = 1;
    private static final String REGION_DELIMITER = " ";
    /** VWorld는 국내 전용이라 주소를 돌려줬다면 국내 좌표가 확정이다. */
    private static final String DOMESTIC_COUNTRY_CODE = "KR";

    private final RestClient restClient;
    private final String apiKey;
    private final DiscordNotifier discordNotifier;

    public VWorldGeocodingClient(
            RestClient.Builder builder,
            @Value("${geocoding.vworld.api-key:}") String apiKey,
            DiscordNotifier discordNotifier
    ) {
        this.apiKey = apiKey;
        this.discordNotifier = discordNotifier;
        this.restClient = builder.baseUrl(VWORLD_BASE_URL).build();
    }

    @Override
    public GeocodingResult reverseGeocode(Double latitude, Double longitude) {
        if (!StringUtils.hasText(apiKey)) {
            log.warn("VWorld 인증키가 설정되지 않아 국내 역지오코딩을 건너뜁니다.");
            return GeocodingResult.failed();
        }

        try {
            return toResult(requestGetAddress(latitude, longitude), latitude, longitude);
        } catch (Exception e) {
            log.error("VWorld 역지오코딩 호출 실패 | lat={}, lng={}", latitude, longitude, e);
            discordNotifier.sendGeocodingFailure(latitude, longitude, e.toString());
            return GeocodingResult.failed();
        }
    }

    /** VWorld는 한국어 단일 응답이라 주소 조회와 국가 조회가 같다. */
    @Override
    public GeocodingResult findCountry(Double latitude, Double longitude) {
        return reverseGeocode(latitude, longitude);
    }

    private VWorldResponse requestGetAddress(Double latitude, Double longitude) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder.path(GET_ADDRESS_PATH)
                        .queryParam("service", "address")
                        .queryParam("request", "getAddress")
                        .queryParam("version", "2.0")
                        .queryParam("crs", "epsg:4326")
                        .queryParam("type", "both")
                        .queryParam("format", "json")
                        .queryParam("point", longitude + "," + latitude)
                        .queryParam("key", apiKey)
                        .build())
                .retrieve()
                .body(VWorldResponse.class);
    }

    /** VWorld는 인증키 오류도 HTTP 200에 status로 담아 보낸다. 주소 없음과 구분하지 않으면 키가 잘못됐을 때 국내 요청이 전부 해외로 넘어간다. */
    private GeocodingResult toResult(VWorldResponse response, Double latitude, Double longitude) {
        VWorldBody body = response == null ? null : response.response();
        if (body == null) {
            String cause = "VWorld 응답 본문이 비어 있음";
            log.error("VWorld 역지오코딩 응답 오류 | lat={}, lng={}, {}", latitude, longitude, cause);
            discordNotifier.sendGeocodingFailure(latitude, longitude, cause);
            return GeocodingResult.failed();
        }
        if (STATUS_NOT_FOUND.equals(body.status())) {
            return GeocodingResult.notFound();
        }
        if (!STATUS_OK.equals(body.status())) {
            String cause = "VWorld status=" + body.status() + ", error=" + body.error();
            log.error("VWorld 역지오코딩 응답 오류 | lat={}, lng={}, {}", latitude, longitude, cause);
            discordNotifier.sendGeocodingFailure(latitude, longitude, cause);
            return GeocodingResult.failed();
        }

        PostAddress postAddress = toPostAddress(body.result());
        if (postAddress.isEmpty()) {
            return GeocodingResult.notFound();
        }
        return GeocodingResult.resolved(postAddress);
    }

    private PostAddress toPostAddress(List<VWorldAddress> addresses) {
        if (addresses == null) {
            return PostAddress.empty();
        }

        VWorldAddress parcel = findByType(addresses, TYPE_PARCEL);
        String parcelText = parcel == null ? null : parcel.text();
        String region = toRegion(parcel);

        VWorldAddress road = findByType(addresses, TYPE_ROAD);
        if (road == null) {
            return PostAddress.of(parcelText, null, null, region, DOMESTIC_COUNTRY_CODE);
        }

        Matcher matcher = ROAD_TEXT.matcher(road.text());
        if (!matcher.matches()) {
            return PostAddress.of(parcelText, road.text(), null, region, DOMESTIC_COUNTRY_CODE);
        }
        return PostAddress.of(parcelText, matcher.group(1), extractBuildingName(matcher.group(2)),
                region, DOMESTIC_COUNTRY_CODE);
    }

    /**
     * 번지를 뺀 동 단위 표기를 만든다. 지역마다 행정 단위 깊이가 달라 원본 주소 문자열을 자르면 위험하지만,
     * structure는 시도·시군구·읍면동이 이미 분리돼 있어 빈 값만 걸러 이어붙이면 된다.
     * (세종특별자치시는 시군구가 비어 있고, 경기도 수원시는 시군구에 "수원시 팔달구"가 함께 들어온다.)
     */
    private String toRegion(VWorldAddress parcel) {
        if (parcel == null || parcel.structure() == null) {
            return null;
        }

        VWorldStructure structure = parcel.structure();
        return Stream.of(structure.level1(), structure.level2(), structure.level4L())
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(REGION_DELIMITER));
    }

    private String extractBuildingName(String parenthesized) {
        String[] parts = parenthesized.split(BUILDING_NAME_DELIMITER);
        if (parts.length <= BUILDING_NAME_INDEX) {
            return null;
        }
        return parts[BUILDING_NAME_INDEX].trim();
    }

    private VWorldAddress findByType(List<VWorldAddress> addresses, String type) {
        return addresses.stream()
                .filter(address -> type.equals(address.type()))
                .findFirst()
                .orElse(null);
    }

    private record VWorldResponse(VWorldBody response) {
    }

    private record VWorldBody(String status, List<VWorldAddress> result, VWorldError error) {
    }

    private record VWorldAddress(String type, String text, VWorldStructure structure) {
    }

    private record VWorldStructure(String level1, String level2, String level4L) {
    }

    private record VWorldError(String code, String text) {
    }
}
