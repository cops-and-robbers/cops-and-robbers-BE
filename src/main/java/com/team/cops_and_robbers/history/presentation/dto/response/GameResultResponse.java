package com.team.cops_and_robbers.history.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.team.cops_and_robbers.game.game.domain.GameMode;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.history.application.dto.result.GameResultResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record GameResultResponse(
        @Schema(description = "게임 모드", example = "COPS_AND_ROBBERS")
        GameMode gameMode,
        @Schema(description = "승리 팀", example = "POLICE")
        Team winnerTeam,
        @Schema(description = "게임 진행 시간(초)", example = "300")
        Integer durationSeconds,
        @Schema(description = "총 체포 횟수", example = "5")
        Integer totalArrestCount,
        @Schema(description = "남은 도둑 수", example = "1")
        Integer remainingRunnerCount,
        @Schema(description = "경찰 팀 MVP. 없으면 null")
        MvpResponse chaserMvp,
        @Schema(description = "도둑 팀 MVP. 없으면 null")
        MvpResponse runnerMvp
) {
    // 옛 앱 호환: 옛 이름으로도 내보낸다. 강제 업데이트 후 제거
    @Deprecated
    @JsonProperty(value = "remainingRobberCount", access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "옛 이름. remainingRunnerCount 을 사용하세요", deprecated = true)
    public Integer remainingRobberCount() {
        return remainingRunnerCount;
    }

    // 옛 앱 호환: 옛 이름으로도 내보낸다. 강제 업데이트 후 제거
    @Deprecated
    @JsonProperty(value = "policeMvp", access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "옛 이름. chaserMvp 을 사용하세요", deprecated = true)
    public MvpResponse policeMvp() {
        return chaserMvp;
    }

    // 옛 앱 호환: 옛 이름으로도 내보낸다. 강제 업데이트 후 제거
    @Deprecated
    @JsonProperty(value = "robberMvp", access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "옛 이름. runnerMvp 을 사용하세요", deprecated = true)
    public MvpResponse robberMvp() {
        return runnerMvp;
    }

    @Schema(name = "MvpResponse", description = "팀 MVP 정보")
    public record MvpResponse(
            @Schema(description = "닉네임 (탈퇴한 유저면 \"알수없음\")", example = "번개경찰")
            String nickname
    ) {
    }

    public static GameResultResponse from(GameResultResult result) {
        return new GameResultResponse(
                result.gameMode(),
                result.winnerTeam(),
                result.durationSeconds(),
                result.totalArrestCount(),
                result.remainingRunnerCount(),
                toMvpResponse(result.chaserMvp()),
                toMvpResponse(result.runnerMvp())
        );
    }

    private static MvpResponse toMvpResponse(GameResultResult.MvpInfo mvp) {
        return mvp == null ? null : new MvpResponse(mvp.nickname());
    }
}
