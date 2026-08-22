package com.team.cops_and_robbers.community.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.exception.InfrastructureException;
import com.team.cops_and_robbers.community.application.dto.result.AddressResult;
import com.team.cops_and_robbers.community.domain.PostAddress;
import com.team.cops_and_robbers.community.exception.CommunityPostException;
import com.team.cops_and_robbers.community.infrastructure.GeocodingResult;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

class AddressServiceTest extends ServiceUnitTest {

    @InjectMocks
    private AddressService addressService;

    @Test
    void 좌표에_해당하는_주소를_반환한다() {
        given(geocodingClient.reverseGeocode(37.5502, 127.0736))
                .willReturn(GeocodingResult.resolved(PostAddress.of(
                        "서울특별시 광진구 화양동 1-20", "서울특별시 광진구 능동로 216", "세종대학교",
                        "서울특별시 광진구 화양동", "KR")));

        AddressResult result = addressService.getAddress(37.5502, 127.0736);

        assertThat(result.region()).isEqualTo("서울특별시 광진구 화양동");
        assertThat(result.address()).isEqualTo("서울특별시 광진구 화양동 1-20");
    }

    @Test
    void 좌표가_속한_국가_코드를_반환한다() {
        given(geocodingClient.findCountry(35.7022, 139.5803))
                .willReturn(GeocodingResult.resolved(PostAddress.of(
                        "吉祥寺大通り", null, null, "東京 武蔵野市 吉祥寺本町", "JP")));

        assertThat(addressService.getCountryCode(35.7022, 139.5803)).isEqualTo("JP");
    }

    @Test
    void 국가를_알_수_없는_좌표는_400으로_거절한다() {
        given(geocodingClient.findCountry(0.0, 0.0)).willReturn(GeocodingResult.notFound());

        assertThatThrownBy(() -> addressService.getCountryCode(0.0, 0.0))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining(CommunityPostException.COUNTRY_NOT_SPECIFIED.getDetail());
    }

    @Test
    void 주소가_없는_좌표는_400으로_거절한다() {
        given(geocodingClient.reverseGeocode(0.0, 0.0)).willReturn(GeocodingResult.notFound());

        assertThatThrownBy(() -> addressService.getAddress(0.0, 0.0))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining(CommunityPostException.ADDRESS_NOT_FOUND.getDetail());
    }

    @Test
    void 조회_실패는_주소_없음과_구분해_500으로_알린다() {
        given(geocodingClient.reverseGeocode(37.5502, 127.0736)).willReturn(GeocodingResult.failed());

        assertThatThrownBy(() -> addressService.getAddress(37.5502, 127.0736))
                .isInstanceOf(InfrastructureException.class)
                .hasMessageContaining(CommunityPostException.ADDRESS_LOOKUP_FAILED.getDetail());
    }
}
