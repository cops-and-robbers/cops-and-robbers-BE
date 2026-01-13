package com.team.cops_and_robbers.game.game.presentation.dto.request;

import com.team.cops_and_robbers.game.game.application.dto.command.GameCreateCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record GameCreateRequest(
        @Valid @NotNull(message = "영역 설정은 필수입니다.")
        AreaRequest area,

        @Valid @NotNull(message = "게임 세부 설정은 필수입니다.")
        GameSettingsRequest settings
) {

    public GameCreateCommand toCommand() {
        return new GameCreateCommand(
                area.playgroundCenter().latitude(),
                area.playgroundCenter().longitude(),
                area.playgroundRadiusInMeters(),
                area.jailCenter().latitude(),
                area.jailCenter().longitude(),
                area.jailRadiusInMeters(),
                settings.roundDurationMinutes(),
                settings.locationRevealIntervalMinutes(),
                settings.policeWaitMinutes(),
                settings.maxParticipants()
        );
    }
}
