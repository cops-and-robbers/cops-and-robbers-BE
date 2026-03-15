package com.team.cops_and_robbers.play.lobby.presentation.dto.request;

import com.team.cops_and_robbers.game.participant.domain.Team;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record TeamChangeRequest(
        @Schema(description = "변경할 팀", example = "POLICE")
        @NotNull(message = "변경할 팀 정보는 필수입니다.")
        Team targetTeam
) {
}
