package com.team.cops_and_robbers.game.game.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.team.cops_and_robbers.game.game.application.dto.result.GameSettingsUpdateResult;
import com.team.cops_and_robbers.game.game.domain.GameMode;
import io.swagger.v3.oas.annotations.media.Schema;

public record GameSettingsUpdateResponse(
        @Schema(description = "게임 모드", example = "COPS_AND_ROBBERS")
        GameMode gameMode,
        @Schema(description = "라운드 시간(분)", example = "60")
        Integer roundDurationMinutes,
        @Schema(description = "위치 공개 주기(분)", example = "10")
        Integer locationRevealIntervalMinutes,
        @Schema(description = "경찰 대기 시간(분)", example = "5")
        Integer chaserWaitMinutes,
        @Schema(description = "최대 참가 인원", example = "20")
        Integer maxParticipants,
        @Schema(description = "구역 축소의 최종 반경(m). null 이면 축소를 쓰지 않는다", example = "200", nullable = true)
        Integer shrinkFinalRadiusInMeters
) {
    // 옛 앱 호환: 옛 이름으로도 내보낸다. 강제 업데이트 후 제거
    @Deprecated
    @JsonProperty(value = "policeWaitMinutes", access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "옛 이름. chaserWaitMinutes 을 사용하세요", deprecated = true)
    public Integer policeWaitMinutes() {
        return chaserWaitMinutes;
    }


    public static GameSettingsUpdateResponse from(GameSettingsUpdateResult result) {
        return new GameSettingsUpdateResponse(
                result.gameMode(),
                result.roundDurationMinutes(),
                result.locationRevealIntervalMinutes(),
                result.chaserWaitMinutes(),
                result.maxParticipants(),
                result.shrinkFinalRadiusInMeters()
        );
    }
}
