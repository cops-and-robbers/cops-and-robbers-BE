package com.team.cops_and_robbers.game.game.application.dto.command;

import com.team.cops_and_robbers.game.area.application.dto.GameAreaData;
import com.team.cops_and_robbers.game.game.domain.GameMode;
import com.team.cops_and_robbers.game.game.presentation.dto.request.GameModeUpdateRequest;

public record GameModeUpdateCommand(
        Long gameId,
        Long userId,
        GameMode gameMode,
        GameAreaData areaData
) {
    public static GameModeUpdateCommand of(Long gameId, Long userId, GameModeUpdateRequest request) {
        return new GameModeUpdateCommand(
                gameId,
                userId,
                request.gameMode(),
                GameAreaUpdateCommand.resolveAreaData(request.area())
        );
    }
}
