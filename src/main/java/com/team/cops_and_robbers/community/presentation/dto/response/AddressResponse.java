package com.team.cops_and_robbers.community.presentation.dto.response;

import com.team.cops_and_robbers.community.application.dto.result.AddressResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record AddressResponse(
        @Schema(description = "게시글에 저장되고 목록에 표시될 동 단위 지역",
                example = "서울특별시 광진구 화양동")
        String region,
        @Schema(description = "번지까지 포함한 지번 주소. 작성자가 위치를 확인하는 용도",
                example = "서울특별시 광진구 화양동 1-20")
        String address,
        @Schema(description = "좌표가 속한 국가 코드(ISO 3166-1 alpha-2). 목록 조회 필터에 그대로 사용한다.",
                example = "KR")
        String countryCode
) {
    public static AddressResponse from(AddressResult result) {
        return new AddressResponse(result.region(), result.address(), result.countryCode());
    }
}
