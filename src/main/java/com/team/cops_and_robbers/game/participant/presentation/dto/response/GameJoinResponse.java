package com.team.cops_and_robbers.game.participant.presentation.dto.response;

import com.team.cops_and_robbers.game.participant.application.dto.result.GameJoinResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record GameJoinResponse(
        @Schema(description = "게임 ID", example = "1")
        Long gameId,
        @Schema(description = "참가자 ID", example = "2")
        Long participantId
) {
    public static GameJoinResponse from(GameJoinResult result) {
        return new GameJoinResponse(
                result.gameId(),
                result.participantId()
        );
    }
}
