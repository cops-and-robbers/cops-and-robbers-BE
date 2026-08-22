package com.team.cops_and_robbers.community.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record CountryResponse(
        @Schema(description = "좌표가 속한 국가 코드(ISO 3166-1 alpha-2). 목록 조회에 그대로 넣으면 된다.",
                example = "KR")
        String countryCode
) {
    public static CountryResponse from(String countryCode) {
        return new CountryResponse(countryCode);
    }
}
