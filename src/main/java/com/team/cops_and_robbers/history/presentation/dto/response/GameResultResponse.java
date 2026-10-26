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
        Integer remainingRobberCount,
        @Schema(description = "경찰 팀 MVP. 없으면 null")
        MvpResponse policeMvp,
        @Schema(description = "도둑 팀 MVP. 없으면 null")
        MvpResponse robberMvp
) {
    @Schema(name = "MvpResponse", description = "팀 MVP 정보")
    public record MvpResponse(
            @Schema(description = "닉네임 (탈퇴한 유저면 \"알수없음\")", example = "번개경찰")
            String nickname
    ) {
    }

    public static GameResultResponse from(GameResultResult result) {
        return new GameResultResponse(
                result.winnerTeam(),
                result.durationSeconds(),
                result.totalArrestCount(),
                result.remainingRobberCount(),
                toMvpResponse(result.policeMvp()),
                toMvpResponse(result.robberMvp())
        );
    }

    private static MvpResponse toMvpResponse(GameResultResult.MvpInfo mvp) {
        return mvp == null ? null : new MvpResponse(mvp.nickname());
    }
}
