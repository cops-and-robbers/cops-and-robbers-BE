package com.team.cops_and_robbers.game.game.presentation.dto.response;

import com.team.cops_and_robbers.game.game.application.dto.result.GameSettingsUpdateResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record GameSettingsUpdateResponse(
        @Schema(description = "라운드 시간(분)", example = "60")
        Integer roundDurationMinutes,
        @Schema(description = "위치 공개 주기(분)", example = "10")
        Integer locationRevealIntervalMinutes,
        @Schema(description = "경찰 대기 시간(분)", example = "5")
        Integer policeWaitMinutes,
        @Schema(description = "최대 참가 인원", example = "20")
        Integer maxParticipants,
        @Schema(description = "구역 축소의 최종 반경(m). null 이면 축소를 쓰지 않는다", example = "200", nullable = true)
        Integer shrinkFinalRadiusInMeters
) {

    public static GameSettingsUpdateResponse from(GameSettingsUpdateResult result) {
        return new GameSettingsUpdateResponse(
                result.roundDurationMinutes(),
                result.locationRevealIntervalMinutes(),
                result.policeWaitMinutes(),
                result.maxParticipants(),
                result.shrinkFinalRadiusInMeters()
        );
    }
}
