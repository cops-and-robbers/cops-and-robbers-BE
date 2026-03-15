package com.team.cops_and_robbers.game.participant.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record GameJoinRequest(
        @Schema(description = "게임 초대 코드", example = "ABC123")
        @NotBlank(message = "초대 코드는 필수입니다.")
        String inviteCode
) {

}
