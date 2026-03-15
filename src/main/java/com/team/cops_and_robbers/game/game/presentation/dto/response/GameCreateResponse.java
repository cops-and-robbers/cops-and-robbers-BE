package com.team.cops_and_robbers.game.game.presentation.dto.response;

import com.team.cops_and_robbers.game.game.application.dto.result.GameCreateResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record GameCreateResponse(
        @Schema(description = "게임 ID", example = "1")
        Long gameId,
        @Schema(description = "초대 코드", example = "ABC123")
        String inviteCode,
        @Schema(description = "게임 상태", example = "WAITING")
        String status,
        @Schema(description = "라운드 시간(분)", example = "30")
        Integer roundDurationMinutes,
        @Schema(description = "위치 공개 주기(분)", example = "5")
        Integer locationRevealIntervalMinutes,
        @Schema(description = "경찰 대기 시간(분)", example = "3")
        Integer policeWaitMinutes,
        @Schema(description = "최대 참가 인원", example = "10")
        Integer maxParticipants,
        @Schema(description = "생성 시각", example = "2026-01-16T01:25:37.543066")
        LocalDateTime createdAt
) {

    public static GameCreateResponse from(GameCreateResult result) {
        return new GameCreateResponse(
                result.gameId(),
                result.inviteCode(),
                result.status(),
                result.roundDurationMinutes(),
                result.locationRevealIntervalMinutes(),
                result.policeWaitMinutes(),
                result.maxParticipants(),
                result.createdAt()
        );
    }
}
