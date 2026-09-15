package com.team.cops_and_robbers.history.presentation.dto.response;

import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.history.application.dto.result.GameResultParticipantResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record GameResultParticipantResponse(
        @Schema(description = "닉네임", example = "살금살금고슴도치")
        String nickname,
        @Schema(description = "팀", example = "POLICE")
        Team team,
        @Schema(description = "종료 시점 상태", example = "ALIVE")
        ParticipantStatus status,
        @Schema(description = "본인이 체포한 횟수", example = "3")
        Integer arrestCount,
        @Schema(description = "게임 중 퇴장한 시각. 끝까지 있었으면 null", example = "2026-09-16T14:30:00")
        String leftAt
) {
    public static GameResultParticipantResponse from(GameResultParticipantResult result) {
        return new GameResultParticipantResponse(
                result.nickname(),
                result.team(),
                result.status(),
                result.arrestCount(),
                result.leftAt()
        );
    }
}
