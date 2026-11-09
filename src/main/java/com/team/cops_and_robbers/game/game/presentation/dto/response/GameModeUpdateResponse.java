package com.team.cops_and_robbers.game.game.presentation.dto.response;

import com.team.cops_and_robbers.game.game.application.dto.result.GameModeUpdateResult;
import com.team.cops_and_robbers.game.game.domain.GameMode;
import io.swagger.v3.oas.annotations.media.Schema;

public record GameModeUpdateResponse(
        @Schema(description = "게임 모드", example = "TAG")
        GameMode gameMode,
        @Schema(description = "게임 세부 설정 정보")
        GameSettingsUpdateResponse settings,
        @Schema(description = "영역 설정 정보")
        GameAreaUpdateResponse area
) {

    public static GameModeUpdateResponse from(GameModeUpdateResult result) {
        return new GameModeUpdateResponse(
                result.gameMode(),
                GameSettingsUpdateResponse.from(result.settings()),
                GameAreaUpdateResponse.from(result.area())
        );
    }
}
