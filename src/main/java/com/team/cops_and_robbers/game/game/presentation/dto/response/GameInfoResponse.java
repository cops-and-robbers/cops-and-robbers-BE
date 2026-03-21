package com.team.cops_and_robbers.game.game.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.team.cops_and_robbers.game.game.application.dto.result.GameInfoResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record GameInfoResponse(
        @Schema(description = "라운드 시간(분)", example = "30")
        Integer roundDurationMinutes,
        @Schema(description = "위치 공개 주기(분)", example = "5")
        Integer locationRevealIntervalMinutes,
        @Schema(description = "경찰 대기 시간(분)", example = "3")
        Integer policeWaitMinutes,
        @Schema(description = "최대 참가 인원", example = "10")
        Integer maxParticipants,
        @Schema(description = "게임 시작 여부", example = "false")
        boolean isStarted,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(description = "게임 시작 시간 (진행 중일 때만 포함)", example = "2026-03-21T15:30:00")
        LocalDateTime gameStartTime
) {
    public static GameInfoResponse from(GameInfoResult result) {
        return new GameInfoResponse(
                result.roundDurationMinutes(),
                result.locationRevealIntervalMinutes(),
                result.policeWaitMinutes(),
                result.maxParticipants(),
                result.isStarted(),
                result.gameStartTime()
        );
    }
}
