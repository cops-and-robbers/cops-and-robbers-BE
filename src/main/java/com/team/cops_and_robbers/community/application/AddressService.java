package com.team.cops_and_robbers.community.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.exception.InfrastructureException;
import com.team.cops_and_robbers.community.application.dto.result.AddressResult;
import com.team.cops_and_robbers.community.exception.CommunityPostException;
import com.team.cops_and_robbers.community.infrastructure.GeocodingClient;
import com.team.cops_and_robbers.community.infrastructure.GeocodingResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 좌표를 주소나 국가로 바꾸는 조회. 저장하지 않는다.
 * <p>
 * 주소는 작성 화면에서 위치를 확인시키는 용도이고, 국가는 목록을 나라별로 거르는 용도다.
 * 목록은 국가만 알면 되므로 표기 언어를 맞추는 추가 호출을 하지 않는다.
 */
@Service
@RequiredArgsConstructor
public class AddressService {

    private final GeocodingClient geocodingClient;

    public String getCountryCode(Double latitude, Double longitude) {
        return switch (geocodingClient.findCountry(latitude, longitude)) {
            case GeocodingResult.Resolved resolved -> resolved.postAddress().countryCode();
            case GeocodingResult.NotFound ignored ->
                    throw new ApplicationException(CommunityPostException.COUNTRY_NOT_SPECIFIED);
            case GeocodingResult.Failed ignored ->
                    throw new InfrastructureException(CommunityPostException.ADDRESS_LOOKUP_FAILED);
        };
    }

    public AddressResult getAddress(Double latitude, Double longitude) {
        return switch (geocodingClient.reverseGeocode(latitude, longitude)) {
            case GeocodingResult.Resolved resolved -> AddressResult.from(resolved.postAddress());
            case GeocodingResult.NotFound ignored ->
                    throw new ApplicationException(CommunityPostException.ADDRESS_NOT_FOUND);
            case GeocodingResult.Failed ignored ->
                    throw new InfrastructureException(CommunityPostException.ADDRESS_LOOKUP_FAILED);
        };
    }
}
