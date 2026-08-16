package com.team.cops_and_robbers.community.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.team.cops_and_robbers.common.infrastructure.discord.DiscordNotifier;
import com.team.cops_and_robbers.community.domain.PostAddress;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class KakaoGeocodingClient implements GeocodingClient {

    private static final String KAKAO_BASE_URL = "https://dapi.kakao.com";
    private static final String COORD_TO_ADDRESS_PATH = "/v2/local/geo/coord2address.json";

    private final RestClient restClient;
    private final String restApiKey;
    private final DiscordNotifier discordNotifier;

    public KakaoGeocodingClient(
            RestClient.Builder builder,
            @Value("${kakao.rest-api-key:}") String restApiKey,
            DiscordNotifier discordNotifier
    ) {
        this.restApiKey = restApiKey;
        this.discordNotifier = discordNotifier;
        this.restClient = builder
                .baseUrl(KAKAO_BASE_URL)
                .defaultHeader("Authorization", "KakaoAK " + restApiKey)
                .build();
    }

    @Override
    public GeocodingResult reverseGeocode(Double latitude, Double longitude) {
        if (!StringUtils.hasText(restApiKey)) {
            log.warn("카카오 REST API 키가 설정되지 않아 역지오코딩을 건너뜁니다.");
            return new GeocodingResult.Failed();
        }

        try {
            PostAddress postAddress = extractPostAddress(requestCoordToAddress(latitude, longitude));
            if (postAddress.isEmpty()) {
                return new GeocodingResult.NotFound();
            }
            return new GeocodingResult.Resolved(postAddress);
        } catch (Exception e) {
            log.warn("카카오 역지오코딩 호출 실패 | lat={}, lng={}", latitude, longitude, e);
            discordNotifier.sendGeocodingFailure(latitude, longitude, e.getMessage());
            return new GeocodingResult.Failed();
        }
    }

    private KakaoCoordToAddressResponse requestCoordToAddress(Double latitude, Double longitude) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder.path(COORD_TO_ADDRESS_PATH)
                        .queryParam("x", longitude)
                        .queryParam("y", latitude)
                        .build())
                .retrieve()
                .body(KakaoCoordToAddressResponse.class);
    }

    private PostAddress extractPostAddress(KakaoCoordToAddressResponse response) {
        if (response == null || response.documents() == null || response.documents().isEmpty()) {
            return PostAddress.empty();
        }

        KakaoDocument document = response.documents().getFirst();
        KakaoAddress address = document.address();
        KakaoRoadAddress roadAddress = document.roadAddress();
        return new PostAddress(
                address == null ? null : address.addressName(),
                roadAddress == null ? null : roadAddress.addressName(),
                roadAddress == null ? null : roadAddress.buildingName()
        );
    }

    private record KakaoCoordToAddressResponse(List<KakaoDocument> documents) {
    }

    private record KakaoDocument(
            KakaoAddress address,
            @JsonProperty("road_address") KakaoRoadAddress roadAddress
    ) {
    }

    private record KakaoAddress(@JsonProperty("address_name") String addressName) {
    }

    private record KakaoRoadAddress(
            @JsonProperty("address_name") String addressName,
            @JsonProperty("building_name") String buildingName
    ) {
    }
}
