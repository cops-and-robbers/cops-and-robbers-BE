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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("Geoapify 역지오코딩 클라이언트")
class GeoapifyGeocodingClientTest {

    private static final String REVERSE_GEOCODE_URL = "https://api.geoapify.com/v1/geocode/reverse";

    private MockRestServiceServer server;
    private DiscordNotifier discordNotifier;
    private GeoapifyGeocodingClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        discordNotifier = mock(DiscordNotifier.class);
        client = new GeoapifyGeocodingClient(builder, "test-key", discordNotifier);
    }

    @Test
    void 해외_좌표의_전체주소와_도로명과_건물명을_담는다() {
        String body = """
                {"results":[{
                  "formatted":"Elizabeth Tower, Bridge Street, London, SW1A 2JR, United Kingdom",
                  "name":"Elizabeth Tower",
                  "street":"Bridge Street",
                  "housenumber":null,
                  "city":"City of Westminster",
                  "suburb":"Millbank"
                }]}
                """;
        server.expect(requestTo(startsWith(REVERSE_GEOCODE_URL)))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        GeocodingResult result = client.reverseGeocode(51.5007, -0.1246);

        assertThat(result).isEqualTo(GeocodingResult.resolved(PostAddress.of(
                "Elizabeth Tower, Bridge Street, London, SW1A 2JR, United Kingdom",
                "Bridge Street",
                "Elizabeth Tower",
                "City of Westminster Millbank")));
    }

    @Test
    void 번지가_있으면_도로명_앞에_붙인다() {
        String body = """
                {"results":[{
                  "formatted":"Sunglass Hut, 1540 Broadway, New York, NY 10036, United States of America",
                  "name":"Sunglass Hut",
                  "street":"Broadway",
                  "housenumber":"1540",
                  "city":"New York",
                  "suburb":"Manhattan"
                }]}
                """;
        server.expect(requestTo(startsWith(REVERSE_GEOCODE_URL)))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        GeocodingResult result = client.reverseGeocode(40.7580, -73.9855);

        assertThat(result).isEqualTo(GeocodingResult.resolved(PostAddress.of(
                "Sunglass Hut, 1540 Broadway, New York, NY 10036, United States of America",
                "1540 Broadway",
                "Sunglass Hut",
                "New York Manhattan")));
    }

    @Test
    void 도시와_하위지역_이름이_같으면_중복을_제거한다() {
        String body = """
                {"results":[{
                  "formatted":"B1F, Shibuya, Shibuya 2 150-0002, Japan",
                  "name":"B1F","street":"B1F","housenumber":null,
                  "city":"Shibuya","suburb":"Shibuya"
                }]}
                """;
        server.expect(requestTo(startsWith(REVERSE_GEOCODE_URL)))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        GeocodingResult result = client.reverseGeocode(35.6595, 139.7016);

        assertThat(result).isEqualTo(GeocodingResult.resolved(PostAddress.of(
                "B1F, Shibuya, Shibuya 2 150-0002, Japan", "B1F", "B1F", "Shibuya")));
    }

    @Test
    void 전체주소가_비어있으면_지형_이름만_있어도_주소_없음으로_본다() {
        String body = """
                {"results":[{"formatted":"","name":"South Pacific Ocean","street":null,"housenumber":null}]}
                """;
        server.expect(requestTo(startsWith(REVERSE_GEOCODE_URL)))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        GeocodingResult result = client.reverseGeocode(-29.8297, -90.5604);

        assertThat(result).isInstanceOf(GeocodingResult.NotFound.class);
    }

    @Test
    void 결과가_비어있으면_주소_없음을_반환한다() {
        server.expect(requestTo(startsWith(REVERSE_GEOCODE_URL)))
                .andRespond(withSuccess("{\"results\":[]}", MediaType.APPLICATION_JSON));

        GeocodingResult result = client.reverseGeocode(0.0, 0.0);

        assertThat(result).isInstanceOf(GeocodingResult.NotFound.class);
    }

    @Test
    void 호출이_실패하면_실패를_반환하고_디스코드로_알린다() {
        server.expect(requestTo(startsWith(REVERSE_GEOCODE_URL)))
                .andRespond(withServerError());

        GeocodingResult result = client.reverseGeocode(51.5007, -0.1246);

        assertThat(result).isInstanceOf(GeocodingResult.Failed.class);
        then(discordNotifier).should().sendGeocodingFailure(any(), any(), any());
    }

    @Test
    void 키가_설정되지_않으면_호출하지_않고_실패를_반환한다() {
        GeoapifyGeocodingClient noKeyClient =
                new GeoapifyGeocodingClient(RestClient.builder(), "", discordNotifier);

        GeocodingResult result = noKeyClient.reverseGeocode(51.5007, -0.1246);

        assertThat(result).isInstanceOf(GeocodingResult.Failed.class);
        then(discordNotifier).shouldHaveNoInteractions();
    }
}
