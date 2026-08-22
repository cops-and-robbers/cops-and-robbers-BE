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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.ExpectedCount.once;
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
    void 해외_좌표의_전체주소와_도로명과_지역과_국가코드를_담는다() {
        server.expect(requestTo(startsWith(REVERSE_GEOCODE_URL)))
                .andRespond(withSuccess("""
                        {"results":[{
                          "formatted":"Elizabeth Tower, Bridge Street, London, SW1A 2JR, United Kingdom",
                          "name":"Elizabeth Tower",
                          "street":"Bridge Street",
                          "housenumber":null,
                          "state":"England",
                          "city":"City of Westminster",
                          "suburb":"Millbank",
                          "country_code":"gb"
                        }]}
                        """, MediaType.APPLICATION_JSON));

        GeocodingResult result = client.reverseGeocode(51.5007, -0.1246);

        assertThat(result).isEqualTo(GeocodingResult.resolved(PostAddress.of(
                "Elizabeth Tower, Bridge Street, London, SW1A 2JR, United Kingdom",
                "Bridge Street",
                null,
                "England City of Westminster Millbank",
                "GB")));
    }

    /** name은 좌표에 가장 가까운 장소라 건물명으로 쓸 수 없다. 타임스퀘어에서 인근 상점이 나온다. */
    @Test
    void 건물명은_담지_않는다() {
        server.expect(requestTo(startsWith(REVERSE_GEOCODE_URL)))
                .andRespond(withSuccess("""
                        {"results":[{
                          "formatted":"Sunglass Hut, 1540 Broadway, New York, NY 10036, United States of America",
                          "name":"Sunglass Hut",
                          "street":"Broadway",
                          "housenumber":"1540",
                          "state":"New York",
                          "city":"New York",
                          "suburb":"Manhattan",
                          "country_code":"us"
                        }]}
                        """, MediaType.APPLICATION_JSON));

        GeocodingResult result = client.reverseGeocode(40.7580, -73.9855);

        assertThat(result).isEqualTo(GeocodingResult.resolved(PostAddress.of(
                "Sunglass Hut, 1540 Broadway, New York, NY 10036, United States of America",
                "1540 Broadway",
                null,
                "New York Manhattan",
                "US")));
    }

    @Test
    void 지원하지_않는_국가는_언어를_지정하지_않고_한_번만_호출한다() {
        server.expect(once(), requestTo(not(containsString("lang="))))
                .andRespond(withSuccess("""
                        {"results":[{
                          "formatted":"5 Avenue Anatole France, 75007 Paris, France",
                          "street":"Avenue Anatole France","housenumber":"5",
                          "state":"Ile-de-France","city":"Paris","suburb":"7th Arrondissement",
                          "country_code":"fr"
                        }]}
                        """, MediaType.APPLICATION_JSON));

        GeocodingResult result = client.reverseGeocode(48.8584, 2.2945);

        assertThat(result).isEqualTo(GeocodingResult.resolved(PostAddress.of(
                "5 Avenue Anatole France, 75007 Paris, France",
                "5 Avenue Anatole France",
                null,
                "Ile-de-France Paris 7th Arrondissement",
                "FR")));
        server.verify();
    }

    /** 기본 응답은 영어라 일본 좌표는 lang=ja로 한 번 더 부른다. */
    @Test
    void 일본_좌표는_일본어로_다시_조회한다() {
        server.expect(once(), requestTo(not(containsString("lang="))))
                .andRespond(withSuccess("""
                        {"results":[{
                          "formatted":"Kichijōji, Musashino, TK 180-0000, Japan",
                          "state":"Tokyo","city":"Musashino","suburb":"Kichijoji Honcho",
                          "country_code":"jp"
                        }]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(containsString("lang=ja")))
                .andRespond(withSuccess("""
                        {"results":[{
                          "formatted":"吉祥寺大通り, 吉祥寺本町, 武蔵野市, TK 180-0000, 日本",
                          "state":"東京","city":"武蔵野市","suburb":"吉祥寺本町",
                          "country_code":"jp"
                        }]}
                        """, MediaType.APPLICATION_JSON));

        GeocodingResult result = client.reverseGeocode(35.7022, 139.5803);

        assertThat(result).isEqualTo(GeocodingResult.resolved(PostAddress.of(
                "吉祥寺大通り, 吉祥寺本町, 武蔵野市, TK 180-0000, 日本",
                null,
                null,
                "東京 武蔵野市 吉祥寺本町",
                "JP")));
        server.verify();
    }

    /** VWorld가 해상이라 놓친 국내 좌표가 여기로 넘어온다. 영어로 두면 국내 목록에 영어 주소가 섞인다. */
    @Test
    void 국내_좌표가_넘어오면_한국어로_다시_조회한다() {
        server.expect(once(), requestTo(not(containsString("lang="))))
                .andRespond(withSuccess("""
                        {"results":[{"formatted":"Seogwipo-si, Jeju, South Korea",
                          "city":"Seogwipo-si","country_code":"kr"}]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(containsString("lang=ko")))
                .andRespond(withSuccess("""
                        {"results":[{"formatted":"서귀포시, 제주특별자치도, 대한민국",
                          "city":"서귀포시","country_code":"kr"}]}
                        """, MediaType.APPLICATION_JSON));

        GeocodingResult result = client.reverseGeocode(33.0, 126.3);

        assertThat(result).isEqualTo(GeocodingResult.resolved(PostAddress.of(
                "서귀포시, 제주특별자치도, 대한민국", null, null, "서귀포시", "KR")));
        server.verify();
    }

    @Test
    void 시도와_도시_이름이_같으면_중복을_제거한다() {
        server.expect(requestTo(startsWith(REVERSE_GEOCODE_URL)))
                .andRespond(withSuccess("""
                        {"results":[{"formatted":"New York, NY, United States of America",
                          "state":"New York","city":"New York","suburb":"Manhattan","country_code":"us"}]}
                        """, MediaType.APPLICATION_JSON));

        GeocodingResult result = client.reverseGeocode(40.7580, -73.9855);

        assertThat(result).isEqualTo(GeocodingResult.resolved(PostAddress.of(
                "New York, NY, United States of America", null, null, "New York Manhattan", "US")));
    }

    /** 목록 조회는 주소를 저장하지 않으므로 일본이어도 언어를 맞추는 2차 호출을 하지 않는다. */
    @Test
    void 국가만_조회하면_일본이어도_한_번만_호출한다() {
        server.expect(once(), requestTo(not(containsString("lang="))))
                .andRespond(withSuccess("""
                        {"results":[{
                          "formatted":"Kichijōji, Musashino, TK 180-0000, Japan",
                          "state":"Tokyo","city":"Musashino","suburb":"Kichijoji Honcho",
                          "country_code":"jp"
                        }]}
                        """, MediaType.APPLICATION_JSON));

        GeocodingResult result = client.findCountry(35.7022, 139.5803);

        assertThat(result).isInstanceOf(GeocodingResult.Resolved.class);
        assertThat(((GeocodingResult.Resolved) result).postAddress().countryCode()).isEqualTo("JP");
        server.verify();
    }

    @Test
    void 전체주소가_비어있으면_지형_이름만_있어도_주소_없음으로_본다() {
        server.expect(requestTo(startsWith(REVERSE_GEOCODE_URL)))
                .andRespond(withSuccess("""
                        {"results":[{"formatted":"","name":"South Pacific Ocean","street":null,"housenumber":null}]}
                        """, MediaType.APPLICATION_JSON));

        GeocodingResult result = client.reverseGeocode(-29.8297, -90.5604);

        assertThat(result).isInstanceOf(GeocodingResult.NotFound.class);
    }

    /** 국가를 모르면 어느 국가 목록에도 안 걸려 작성자조차 못 찾는 글이 된다. */
    @Test
    void 국가_코드가_없으면_주소가_있어도_주소_없음으로_본다() {
        server.expect(requestTo(startsWith(REVERSE_GEOCODE_URL)))
                .andRespond(withSuccess("""
                        {"results":[{"formatted":"Some Place, Antarctica","state":"Antarctica"}]}
                        """, MediaType.APPLICATION_JSON));

        GeocodingResult result = client.reverseGeocode(-82.0, 0.0);

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