package com.team.cops_and_robbers.game.game.application.dto.result;

import com.team.cops_and_robbers.game.area.domain.GameArea;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.domain.GameMode;

public record GameModeUpdateResult(
        GameMode gameMode,
        GameSettingsUpdateResult settings,
        GameAreaUpdateResult area
) {

    public static GameModeUpdateResult of(Game game, GameArea gameArea) {
        return new GameModeUpdateResult(
                game.getGameMode(),
                GameSettingsUpdateResult.from(game),
                GameAreaUpdateResult.from(gameArea)
        );
    }
}
