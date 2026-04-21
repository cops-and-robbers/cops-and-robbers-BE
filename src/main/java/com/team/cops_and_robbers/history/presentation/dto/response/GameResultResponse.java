package com.team.cops_and_robbers.history.presentation.dto.response;

import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.history.application.dto.result.GameResultResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record GameResultResponse(
        @Schema(description = "승리 팀", example = "POLICE")
        Team winnerTeam,
        @Schema(description = "게임 진행 시간(초)", example = "300")
        Integer durationSeconds,
        @Schema(description = "총 체포 횟수", example = "5")
        Integer totalArrestCount,
        @Schema(description = "남은 도둑 수", example = "1")
        Integer remainingRobberCount
) {
    public static GameResultResponse from(GameResultResult result) {
        return new GameResultResponse(
                result.winnerTeam(),
                result.durationSeconds(),
                result.totalArrestCount(),
                result.remainingRobberCount()
        );
    }
}
