package com.team.cops_and_robbers.user.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.team.cops_and_robbers.game.game.domain.GameStatus;
import com.team.cops_and_robbers.user.application.dto.result.UserGameInfoResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record UserGameInfoResponse(
        @Schema(description = "게임 참여 여부", example = "true")
        boolean isParticipating,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(description = "참여 중인 게임 정보 (미참여 시 미포함)")
        UserGameParticipationResponse participationInfo
) {
    @Schema(name = "UserGameParticipationResponse", description = "유저 게임 참여 정보")
    public record UserGameParticipationResponse(
            @Schema(description = "게임 ID", example = "3")
            Long gameId,
            @Schema(description = "참가자 ID", example = "12")
            Long participantId,
            @Schema(description = "게임 상태", example = "WAITING")
            GameStatus gameStatus
    ) {}

    public static UserGameInfoResponse from(UserGameInfoResult result) {
        UserGameParticipationResponse participation = new UserGameParticipationResponse(
                result.gameId(), result.participantId(), result.gameStatus()
        );
        return new UserGameInfoResponse(true, participation);
    }

    public static UserGameInfoResponse notParticipating() {
        return new UserGameInfoResponse(false, null);
    }
}
