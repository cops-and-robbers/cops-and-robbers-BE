package com.team.cops_and_robbers.game.participant.presentation.dto.response;

import com.team.cops_and_robbers.game.participant.application.dto.result.GameLeaveResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record GameLeaveResponse(
        @Schema(description = "퇴장한 유저 ID", example = "2")
        Long leftUserId,
        @Schema(description = "남은 참가자 수", example = "5")
        Integer remainingCount
) {
    public static GameLeaveResponse from(GameLeaveResult result) {
        return new GameLeaveResponse(
                result.leftUserId(),
                result.remainingCount()
        );
    }
}
