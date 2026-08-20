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
 * 게시글 작성 화면에서 좌표에 해당하는 주소를 미리 보여주기 위한 조회. 저장하지 않는다.
 * <p>
 * 게시글 생성 시점에 변환하면 작성자가 위치를 확인할 방법이 없어 조회를 분리했다.
 */
@Service
@RequiredArgsConstructor
public class AddressService {

    private final GeocodingClient geocodingClient;

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
