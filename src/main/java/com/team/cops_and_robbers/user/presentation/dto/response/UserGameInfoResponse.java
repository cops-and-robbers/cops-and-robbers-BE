package com.team.cops_and_robbers.user.presentation.dto.response;

import com.team.cops_and_robbers.user.application.dto.result.UserGameInfoResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record UserGameInfoResponse(
        @Schema(description = "게임 ID", example = "3")
        Long gameId,
        @Schema(description = "참가자 ID", example = "12")
        Long participantId
) {
    public static UserGameInfoResponse from(UserGameInfoResult result) {
        return new UserGameInfoResponse(result.gameId(), result.participantId());
    }
}
