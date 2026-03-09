package com.team.cops_and_robbers.game.game.presentation.dto.response;

import com.team.cops_and_robbers.common.dto.Coordinates;
import com.team.cops_and_robbers.game.game.application.dto.result.GameAreaUpdateResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record GameAreaUpdateResponse(
        @Schema(description = "플레이그라운드 중심 좌표")
        Coordinates playgroundCenter,
        @Schema(description = "플레이그라운드 반경(m)", example = "1000")
        Integer playgroundRadiusInMeters,
        @Schema(description = "감옥 중심 좌표")
        Coordinates jailCenter,
        @Schema(description = "감옥 반경(m)", example = "100")
        Integer jailRadiusInMeters
) {
    public static GameAreaUpdateResponse from(GameAreaUpdateResult result) {
        return new GameAreaUpdateResponse(
                result.playgroundCenter(),
                result.playgroundRadiusInMeters(),
                result.jailCenter(),
                result.jailRadiusInMeters()
        );
    }
}
