package com.team.cops_and_robbers.play.location.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.team.cops_and_robbers.game.game.domain.GameMode;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.play.location.application.dto.result.GameStateResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record GameStateResponse(
        @Schema(description = "게임 모드", example = "COPS_AND_ROBBERS")
        GameMode gameMode,

        @Schema(description = "마지막 공개된 생존 도둑 위치 목록")
        List<RunnerLocationInfo> runnerLocations,

        @Schema(description = "전체 참여자 현황")
        List<ParticipantInfo> participants
) {
    // 옛 앱 호환: 옛 이름으로도 내보낸다. 강제 업데이트 후 제거
    @Deprecated
    @JsonProperty(value = "robberLocations", access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "옛 이름. runnerLocations 을 사용하세요", deprecated = true)
    public List<RunnerLocationInfo> robberLocations() {
        return runnerLocations;
    }

    public record RunnerLocationInfo(
            @Schema(description = "참여자 ID") Long participantId,
            @Schema(description = "닉네임") String nickname,
            @Schema(description = "위도") Double latitude,
            @Schema(description = "경도") Double longitude
    ) {
        public static RunnerLocationInfo from(GameStateResult.RunnerLocationInfo result) {
            return new RunnerLocationInfo(
                    result.participantId(),
                    result.nickname(),
                    result.latitude(),
                    result.longitude()
            );
        }
    }

    public record ParticipantInfo(
            @Schema(description = "참여자 ID") Long participantId,
            @Schema(description = "닉네임") String nickname,
            @Schema(description = "팀") Team team,
            @Schema(description = "상태") ParticipantStatus status
    ) {
        public static ParticipantInfo from(GameStateResult.ParticipantInfo result) {
            return new ParticipantInfo(
                    result.participantId(),
                    result.nickname(),
                    result.team(),
                    result.status()
            );
        }
    }

    public static GameStateResponse from(GameStateResult result) {
        return new GameStateResponse(
                result.gameMode(),
                result.runnerLocations().stream().map(RunnerLocationInfo::from).toList(),
                result.participants().stream().map(ParticipantInfo::from).toList()
        );
    }
}
