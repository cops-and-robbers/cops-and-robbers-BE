package com.team.cops_and_robbers.play.location.presentation.dto;

import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.play.location.application.dto.result.GameStateResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record GameStateResponse(
        @Schema(description = "마지막 공개된 생존 도둑 위치 목록")
        List<RobberLocationInfo> robberLocations,

        @Schema(description = "전체 참여자 현황")
        List<ParticipantInfo> participants
) {
    public record RobberLocationInfo(
            @Schema(description = "참여자 ID") Long participantId,
            @Schema(description = "닉네임") String nickname,
            @Schema(description = "위도") Double latitude,
            @Schema(description = "경도") Double longitude
    ) {
        public static RobberLocationInfo from(GameStateResult.RobberLocationInfo result) {
            return new RobberLocationInfo(
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
                result.robberLocations().stream().map(RobberLocationInfo::from).toList(),
                result.participants().stream().map(ParticipantInfo::from).toList()
        );
    }
}
