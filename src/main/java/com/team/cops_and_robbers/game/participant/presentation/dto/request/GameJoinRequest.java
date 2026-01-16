package com.team.cops_and_robbers.game.participant.presentation.dto.request;

import com.team.cops_and_robbers.game.participant.application.dto.command.GameJoinCommand;
import jakarta.validation.constraints.NotBlank;

public record GameJoinRequest(
        @NotBlank(message = "초대 코드는 필수입니다.")
        String inviteCode
) {
    public GameJoinCommand toCommand() {
        return new GameJoinCommand(inviteCode);
    }
}
