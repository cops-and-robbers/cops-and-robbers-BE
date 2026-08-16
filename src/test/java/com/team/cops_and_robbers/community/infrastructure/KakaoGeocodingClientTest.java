package com.team.cops_and_robbers.community.infrastructure;

import com.team.cops_and_robbers.common.infrastructure.discord.DiscordNotifier;
import com.team.cops_and_robbers.community.domain.PostAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("카카오 역지오코딩 클라이언트")
class KakaoGeocodingClientTest {

    private static final String COORD_TO_ADDRESS_URL = "https://dapi.kakao.com/v2/local/geo/coord2address.json";

    private MockRestServiceServer server;
    private DiscordNotifier discordNotifier;
    private KakaoGeocodingClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        discordNotifier = mock(DiscordNotifier.class);
        client = new KakaoGeocodingClient(builder, "test-key", discordNotifier);
    }

    @Test
    void 좌표로_지번과_도로명과_건물명을_조회한다() {
        String body = """
                {"documents":[{
                  "address":{"address_name":"서울 광진구 군자동 98"},
                  "road_address":{"address_name":"서울특별시 광진구 능동로 209","building_name":"세종대학교"}
                }]}
                """;
        server.expect(requestTo(startsWith(COORD_TO_ADDRESS_URL)))
                .andExpect(header("Authorization", "KakaoAK test-key"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        GeocodingResult result = client.reverseGeocode(37.5502, 127.0736);

        assertThat(result).isEqualTo(new GeocodingResult.Resolved(
                new PostAddress("서울 광진구 군자동 98", "서울특별시 광진구 능동로 209", "세종대학교")));
    }

    @Test
    void 도로명_주소가_없는_좌표는_지번만_담는다() {
        String body = """
                {"documents":[{"address":{"address_name":"서울 서초구 서초동 1373"},"road_address":null}]}
                """;
        server.expect(requestTo(startsWith(COORD_TO_ADDRESS_URL)))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        GeocodingResult result = client.reverseGeocode(37.4979, 127.0276);

        assertThat(result).isEqualTo(new GeocodingResult.Resolved(
                new PostAddress("서울 서초구 서초동 1373", null, null)));
    }

    @Test
    void 건물명이_빈_문자열이면_null로_담는다() {
        String body = """
                {"documents":[{
                  "address":{"address_name":"서울 광진구 자양동 12"},
                  "road_address":{"address_name":"서울특별시 광진구 아차산로 100","building_name":""}
                }]}
                """;
        server.expect(requestTo(startsWith(COORD_TO_ADDRESS_URL)))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        GeocodingResult result = client.reverseGeocode(37.5311, 127.0663);

        assertThat(result).isEqualTo(new GeocodingResult.Resolved(
                new PostAddress("서울 광진구 자양동 12", "서울특별시 광진구 아차산로 100", null)));
    }

    @Test
    void 지번_주소가_없어도_도로명이_있으면_주소를_반환한다() {
        String body = """
                {"documents":[{
                  "address":null,
                  "road_address":{"address_name":"세종특별자치시 한누리대로 2130","building_name":"세종시청"}
                }]}
                """;
        server.expect(requestTo(startsWith(COORD_TO_ADDRESS_URL)))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        GeocodingResult result = client.reverseGeocode(36.4800, 127.2890);

        assertThat(result).isEqualTo(new GeocodingResult.Resolved(
                new PostAddress(null, "세종특별자치시 한누리대로 2130", "세종시청")));
    }

    @Test
    void 결과가_비어있으면_주소_없음을_반환한다() {
        server.expect(requestTo(startsWith(COORD_TO_ADDRESS_URL)))
                .andRespond(withSuccess("{\"documents\":[]}", MediaType.APPLICATION_JSON));

        GeocodingResult result = client.reverseGeocode(0.0, 0.0);

        assertThat(result).isInstanceOf(GeocodingResult.NotFound.class);
    }

    @Test
    void 호출이_실패하면_실패를_반환하고_디스코드로_알린다() {
        server.expect(requestTo(startsWith(COORD_TO_ADDRESS_URL)))
                .andRespond(withServerError());

        GeocodingResult result = client.reverseGeocode(37.5311, 127.0663);

        assertThat(result).isInstanceOf(GeocodingResult.Failed.class);
        then(discordNotifier).should().sendGeocodingFailure(any(), any(), any());
    }

    @Test
    void 키가_설정되지_않으면_호출하지_않고_실패를_반환한다() {
        KakaoGeocodingClient noKeyClient = new KakaoGeocodingClient(RestClient.builder(), "", discordNotifier);

        GeocodingResult result = noKeyClient.reverseGeocode(37.5311, 127.0663);

        assertThat(result).isInstanceOf(GeocodingResult.Failed.class);
        then(discordNotifier).shouldHaveNoInteractions();
    }
}
