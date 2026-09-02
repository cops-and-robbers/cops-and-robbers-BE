package com.team.cops_and_robbers.community.post.infrastructure;

import com.team.cops_and_robbers.common.infrastructure.discord.DiscordNotifier;
import com.team.cops_and_robbers.community.post.domain.PostAddress;
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

@DisplayName("VWorld 역지오코딩 클라이언트")
class VWorldGeocodingClientTest {

    private static final String GET_ADDRESS_URL = "https://api.vworld.kr/req/address";

    private MockRestServiceServer server;
    private DiscordNotifier discordNotifier;
    private VWorldGeocodingClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        discordNotifier = mock(DiscordNotifier.class);
        client = new VWorldGeocodingClient(builder, "test-key", discordNotifier);
    }

    @Test
    void 지번과_도로명이_모두_있으면_괄호에서_건물명까지_분리한다() {
        String body = """
                {"response":{"status":"OK","result":[
                  {"type":"parcel","text":"서울특별시 송파구 신천동 29",
                   "structure":{"level1":"서울특별시","level2":"송파구","level4L":"신천동"}},
                  {"type":"road","text":"서울특별시 송파구 올림픽로 300 (신천동,롯데월드타워앤드롯데월드몰)"}
                ]}}
                """;
        server.expect(requestTo(startsWith(GET_ADDRESS_URL)))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        GeocodingResult result = client.reverseGeocode(37.5125, 127.1025);

        assertThat(result).isEqualTo(GeocodingResult.resolved(PostAddress.of(
                "서울특별시 송파구 신천동 29",
                "서울특별시 송파구 올림픽로 300",
                "롯데월드타워앤드롯데월드몰", "서울특별시 송파구 신천동", "KR")));
    }

    @Test
    void 괄호에_법정동만_있으면_건물명은_null이_된다() {
        String body = """
                {"response":{"status":"OK","result":[
                  {"type":"parcel","text":"서울특별시 성동구 성수동2가 289-30",
                   "structure":{"level1":"서울특별시","level2":"성동구","level4L":"성수동2가"}},
                  {"type":"road","text":"서울특별시 성동구 아차산로 100 (성수동2가)"}
                ]}}
                """;
        server.expect(requestTo(startsWith(GET_ADDRESS_URL)))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        GeocodingResult result = client.reverseGeocode(37.5447, 127.0557);

        assertThat(result).isEqualTo(GeocodingResult.resolved(PostAddress.of(
                "서울특별시 성동구 성수동2가 289-30", "서울특별시 성동구 아차산로 100", null, "서울특별시 성동구 성수동2가", "KR")));
    }

    @Test
    void 도로명이_없으면_지번만_담는다() {
        String body = """
                {"response":{"status":"OK","result":[
                  {"type":"parcel","text":"서울특별시 광진구 군자동 98",
                   "structure":{"level1":"서울특별시","level2":"광진구","level4L":"군자동"}}
                ]}}
                """;
        server.expect(requestTo(startsWith(GET_ADDRESS_URL)))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        GeocodingResult result = client.reverseGeocode(37.5502, 127.0736);

        assertThat(result).isEqualTo(GeocodingResult.resolved(
                PostAddress.of("서울특별시 광진구 군자동 98", null, null, "서울특별시 광진구 군자동", "KR")));
    }

    @Test
    void 시군구가_없는_세종시는_시도와_동만_이어붙인다() {
        String body = """
                {"response":{"status":"OK","result":[
                  {"type":"parcel","text":"세종특별자치시  보람동 718",
                   "structure":{"level1":"세종특별자치시","level2":"","level4L":"보람동"}}
                ]}}
                """;
        server.expect(requestTo(startsWith(GET_ADDRESS_URL)))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        GeocodingResult result = client.reverseGeocode(36.4800, 127.2890);

        assertThat(result).isEqualTo(GeocodingResult.resolved(
                PostAddress.of("세종특별자치시  보람동 718", null, null, "세종특별자치시 보람동", "KR")));
    }

    @Test
    void 시_아래_구가_있는_지역은_시군구를_그대로_사용한다() {
        String body = """
                {"response":{"status":"OK","result":[
                  {"type":"parcel","text":"경기도 수원시 팔달구 인계동 1111",
                   "structure":{"level1":"경기도","level2":"수원시 팔달구","level4L":"인계동"}}
                ]}}
                """;
        server.expect(requestTo(startsWith(GET_ADDRESS_URL)))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        GeocodingResult result = client.reverseGeocode(37.2636, 127.0286);

        assertThat(result).isEqualTo(GeocodingResult.resolved(PostAddress.of(
                "경기도 수원시 팔달구 인계동 1111", null, null, "경기도 수원시 팔달구 인계동", "KR")));
    }

    @Test
    void 읍면_지역도_같은_방식으로_조합한다() {
        String body = """
                {"response":{"status":"OK","result":[
                  {"type":"parcel","text":"강원특별자치도 홍천군 내면 창촌리 산 3",
                   "structure":{"level1":"강원특별자치도","level2":"홍천군","level4L":"내면"}}
                ]}}
                """;
        server.expect(requestTo(startsWith(GET_ADDRESS_URL)))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        GeocodingResult result = client.reverseGeocode(37.7500, 128.4500);

        assertThat(result).isEqualTo(GeocodingResult.resolved(PostAddress.of(
                "강원특별자치도 홍천군 내면 창촌리 산 3", null, null, "강원특별자치도 홍천군 내면", "KR")));
    }

    @Test
    void 국내가_아닌_좌표는_주소_없음을_반환한다() {
        String body = """
                {"response":{"status":"NOT_FOUND"}}
                """;
        server.expect(requestTo(startsWith(GET_ADDRESS_URL)))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        GeocodingResult result = client.reverseGeocode(35.6595, 139.7016);

        assertThat(result).isInstanceOf(GeocodingResult.NotFound.class);
        then(discordNotifier).shouldHaveNoInteractions();
    }

    @Test
    void 인증키_오류는_주소_없음이_아니라_실패로_처리한다() {
        String body = """
                {"response":{"status":"ERROR","error":{"code":"INVALID_KEY","text":"등록되지 않은 인증키입니다."}}}
                """;
        server.expect(requestTo(startsWith(GET_ADDRESS_URL)))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        GeocodingResult result = client.reverseGeocode(37.5502, 127.0736);

        assertThat(result).isInstanceOf(GeocodingResult.Failed.class);
        then(discordNotifier).should().sendGeocodingFailure(any(), any(), any());
    }

    @Test
    void 호출이_실패하면_실패를_반환하고_디스코드로_알린다() {
        server.expect(requestTo(startsWith(GET_ADDRESS_URL)))
                .andRespond(withServerError());

        GeocodingResult result = client.reverseGeocode(37.5502, 127.0736);

        assertThat(result).isInstanceOf(GeocodingResult.Failed.class);
        then(discordNotifier).should().sendGeocodingFailure(any(), any(), any());
    }

    @Test
    void 키가_설정되지_않으면_호출하지_않고_실패를_반환한다() {
        VWorldGeocodingClient noKeyClient =
                new VWorldGeocodingClient(RestClient.builder(), "", discordNotifier);

        GeocodingResult result = noKeyClient.reverseGeocode(37.5502, 127.0736);

        assertThat(result).isInstanceOf(GeocodingResult.Failed.class);
        then(discordNotifier).shouldHaveNoInteractions();
    }
}
