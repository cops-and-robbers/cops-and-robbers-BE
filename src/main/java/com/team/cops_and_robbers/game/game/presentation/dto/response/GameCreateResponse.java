package com.team.cops_and_robbers.game.game.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.team.cops_and_robbers.game.game.application.dto.result.GameCreateResult;
import com.team.cops_and_robbers.game.game.domain.GameMode;
import io.swagger.v3.oas.annotations.media.Schema;
public record GameCreateResponse(
        @Schema(description = "게임 ID", example = "1")
        Long gameId,
        @Schema(description = "초대 코드", example = "ABC123")
        String inviteCode,
        @Schema(description = "게임 상태", example = "WAITING")
        String status,
        @Schema(description = "게임 모드", example = "COPS_AND_ROBBERS")
        GameMode gameMode,
        @Schema(description = "라운드 시간(분)", example = "30")
        Integer roundDurationMinutes,
        @Schema(description = "위치 공개 주기(분)", example = "5")
        Integer locationRevealIntervalMinutes,
        @Schema(description = "경찰 대기 시간(분)", example = "3")
        Integer chaserWaitMinutes,
        @Schema(description = "최대 참가 인원", example = "10")
        Integer maxParticipants,
        @Schema(description = "구역 축소의 최종 반경(m). null 이면 축소를 쓰지 않는다", example = "200", nullable = true)
        Integer shrinkFinalRadiusInMeters,
        @Schema(description = "생성 시각", example = "2026-01-16T01:25:37+09:00")
        String createdAt
) {
    // 옛 앱 호환: 옛 이름으로도 내보낸다. 강제 업데이트 후 제거
    @Deprecated
    @JsonProperty(value = "policeWaitMinutes", access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "옛 이름. chaserWaitMinutes 을 사용하세요", deprecated = true)
    public Integer policeWaitMinutes() {
        return chaserWaitMinutes;
    }


    public static GameCreateResponse from(GameCreateResult result) {
        return new GameCreateResponse(
                result.gameId(),
                result.inviteCode(),
                result.status(),
                result.gameMode(),
                result.roundDurationMinutes(),
                result.locationRevealIntervalMinutes(),
                result.chaserWaitMinutes(),
                result.maxParticipants(),
                result.shrinkFinalRadiusInMeters(),
                result.createdAt()
        );
    }
}
