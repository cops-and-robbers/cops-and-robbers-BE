package com.team.cops_and_robbers.game.participant.presentation.dto.response;

import com.team.cops_and_robbers.game.participant.application.dto.result.GameJoinResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record GameJoinResponse(
        @Schema(description = "게임 ID", example = "1")
        Long gameId,
        @Schema(description = "참가자 ID", example = "2")
        Long participantId,
        @Schema(description = "이벤트 게임 여부 (true면 인게임 직접 입장)", example = "false")
        boolean isEventGame
) {
    public static GameJoinResponse from(GameJoinResult result) {
        return new GameJoinResponse(
                result.gameId(),
                result.participantId(),
                result.isEventGame()
        );
    }
}
