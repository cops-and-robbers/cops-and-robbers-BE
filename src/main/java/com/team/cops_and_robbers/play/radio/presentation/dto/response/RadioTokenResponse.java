package com.team.cops_and_robbers.play.radio.presentation.dto.response;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.play.radio.application.dto.result.RadioTokenResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record RadioTokenResponse(
        @Schema(description = "무전 서버 접속 주소", example = "wss://voice.copsnro66ers.site")
        String url,
        @Schema(description = "접속할 무전 방 이름", example = "game-1-police")
        String room,
        @Schema(description = "무전 방에서의 식별자. 참가자 ID 입니다", example = "101")
        String identity,
        @Schema(description = "무전 서버 접속 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
        String token,
        @Schema(description = "토큰 만료 시각. 이 시각 전에 재발급받아야 합니다", example = "2026-11-09T21:30:00+09:00")
        String expiresAt
) {
    public static RadioTokenResponse from(RadioTokenResult result) {
        return new RadioTokenResponse(
                result.url(),
                result.room(),
                result.identity(),
                result.token(),
                TimestampUtil.toIsoString(result.expiresAt())
        );
    }
}
