package com.team.cops_and_robbers.community.post.infrastructure;

import com.team.cops_and_robbers.community.post.domain.PostAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@DisplayName("국내·해외 역지오코딩 라우팅")
class RoutingGeocodingClientTest {

    private static final PostAddress DOMESTIC_ADDRESS =
            PostAddress.of("서울특별시 광진구 군자동 98", null, null, "서울특별시 광진구 군자동", "KR");
    private static final PostAddress OVERSEAS_ADDRESS =
            PostAddress.of("Elizabeth Tower, Bridge Street, London", "Bridge Street", null,
                    "England City of Westminster Millbank", "GB");

    private VWorldGeocodingClient domesticClient;
    private GeoapifyGeocodingClient overseasClient;
    private RoutingGeocodingClient client;

    @BeforeEach
    void setUp() {
        domesticClient = mock(VWorldGeocodingClient.class);
        overseasClient = mock(GeoapifyGeocodingClient.class);
        client = new RoutingGeocodingClient(domesticClient, overseasClient);
    }

    @Test
    void 국내에서_주소를_찾으면_해외를_호출하지_않는다() {
        given(domesticClient.reverseGeocode(any(), any()))
                .willReturn(GeocodingResult.resolved(DOMESTIC_ADDRESS));

        GeocodingResult result = client.reverseGeocode(37.5502, 127.0736);

        assertThat(result).isEqualTo(GeocodingResult.resolved(DOMESTIC_ADDRESS));
        then(overseasClient).shouldHaveNoInteractions();
    }

    @Test
    void 국내에_주소가_없으면_해외로_넘긴다() {
        given(domesticClient.reverseGeocode(any(), any())).willReturn(GeocodingResult.notFound());
        given(overseasClient.reverseGeocode(any(), any()))
                .willReturn(GeocodingResult.resolved(OVERSEAS_ADDRESS));

        GeocodingResult result = client.reverseGeocode(51.5007, -0.1246);

        assertThat(result).isEqualTo(GeocodingResult.resolved(OVERSEAS_ADDRESS));
    }

    @Test
    void 국내_호출이_실패해도_해외가_대신_처리한다() {
        given(domesticClient.reverseGeocode(any(), any())).willReturn(GeocodingResult.failed());
        given(overseasClient.reverseGeocode(any(), any()))
                .willReturn(GeocodingResult.resolved(OVERSEAS_ADDRESS));

        GeocodingResult result = client.reverseGeocode(37.5502, 127.0736);

        assertThat(result).isEqualTo(GeocodingResult.resolved(OVERSEAS_ADDRESS));
    }

    @Test
    void 양쪽_모두_주소가_없으면_주소_없음을_반환한다() {
        given(domesticClient.reverseGeocode(any(), any())).willReturn(GeocodingResult.notFound());
        given(overseasClient.reverseGeocode(any(), any())).willReturn(GeocodingResult.notFound());

        GeocodingResult result = client.reverseGeocode(0.0, 0.0);

        assertThat(result).isInstanceOf(GeocodingResult.NotFound.class);
    }

    @Test
    void 국토_밖_좌표는_국내를_거치지_않고_해외로_간다() {
        given(overseasClient.findCountry(any(), any()))
                .willReturn(GeocodingResult.resolved(OVERSEAS_ADDRESS));

        client.findCountry(35.6812, 139.767);

        then(domesticClient).shouldHaveNoInteractions();
    }

    @Test
    void 독도는_국내로_판정해_국내를_먼저_부른다() {
        given(domesticClient.findCountry(any(), any()))
                .willReturn(GeocodingResult.resolved(DOMESTIC_ADDRESS));

        client.findCountry(37.2412, 131.8646);

        then(overseasClient).shouldHaveNoInteractions();
    }

    @Test
    void 마라도는_국내로_판정해_국내를_먼저_부른다() {
        given(domesticClient.findCountry(any(), any()))
                .willReturn(GeocodingResult.resolved(DOMESTIC_ADDRESS));

        client.findCountry(33.1118, 126.2683);

        then(overseasClient).shouldHaveNoInteractions();
    }

    @Test
    void 양쪽_모두_실패하면_실패를_반환한다() {
        given(domesticClient.reverseGeocode(any(), any())).willReturn(GeocodingResult.failed());
        given(overseasClient.reverseGeocode(any(), any())).willReturn(GeocodingResult.failed());

        GeocodingResult result = client.reverseGeocode(37.5502, 127.0736);

        assertThat(result).isInstanceOf(GeocodingResult.Failed.class);
    }
}
