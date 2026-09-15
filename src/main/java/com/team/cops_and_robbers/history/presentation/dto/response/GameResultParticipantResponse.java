package com.team.cops_and_robbers.history.presentation.dto.response;

import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.history.application.dto.result.GameResultParticipantResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record GameResultParticipantResponse(
        @Schema(description = "닉네임", example = "살금살금고슴도치")
        String nickname,
        @Schema(description = "팀", example = "POLICE")
        Team team,
        @Schema(description = "종료 시점 상태", example = "ALIVE")
        ParticipantStatus status,
        @Schema(description = "본인이 체포한 횟수. 이 기능 이전 기록은 null", example = "3")
        Integer arrestCount,
        @Schema(description = "게임 중 퇴장한 시각. 끝까지 있었으면 null")
        LocalDateTime leftAt
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
