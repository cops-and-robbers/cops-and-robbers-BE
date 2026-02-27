package com.team.cops_and_robbers.game.game.application.dto.command;

import com.team.cops_and_robbers.game.game.presentation.dto.request.GameSettingsRequest;

public record GameSettingsUpdateCommand(
        Long gameId,
        Long userId,
        Integer roundDurationMinutes,
        Integer locationRevealIntervalMinutes,
        Integer policeWaitMinutes,
        Integer maxParticipants
) {

    public static GameSettingsUpdateCommand of(Long gameId, Long userId, GameSettingsRequest settings) {
        return new GameSettingsUpdateCommand(
                gameId,
                userId,
                settings.roundDurationMinutes(),
                settings.locationRevealIntervalMinutes(),
                settings.policeWaitMinutes(),
                settings.maxParticipants()
        );
    }
}
