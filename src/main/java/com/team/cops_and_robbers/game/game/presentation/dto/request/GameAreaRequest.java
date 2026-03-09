package com.team.cops_and_robbers.game.game.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GameAreaRequest(
        @Schema(description = "플레이그라운드 중심 좌표")
        @Valid @NotNull(message = "플레이그라운드 중심 좌표를 입력해주세요.")
        CoordinatesRequest playgroundCenter,

        @Schema(description = "플레이그라운드 반경(m)", example = "1000")
        @NotNull(message = "플레이그라운드 반경을 입력해주세요.")
        @Min(value = 10, message = "반경은 최소 10m 이상이어야 합니다.")
        Integer playgroundRadiusInMeters,

        @Schema(description = "감옥 중심 좌표")
        @Valid @NotNull(message = "감옥 중심 좌표를 입력해주세요.")
        CoordinatesRequest jailCenter,

        @Schema(description = "감옥 반경(m)", example = "100")
        @NotNull(message = "감옥 반경을 입력해주세요.")
        @Min(value = 5, message = "감옥 반경은 최소 5m 이상이어야 합니다.")
        Integer jailRadiusInMeters
) {

}
