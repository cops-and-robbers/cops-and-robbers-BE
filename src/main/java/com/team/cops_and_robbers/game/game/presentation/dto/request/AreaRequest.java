package com.team.cops_and_robbers.game.game.presentation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AreaRequest(
        @Valid @NotNull(message = "플레이그라운드 중심 좌표를 입력해주세요.")
        CoordinatesRequest playgroundCenter,

        @NotNull(message = "플레이그라운드 반경을 입력해주세요.")
        @Min(value = 10, message = "반경은 최소 10m 이상이어야 합니다.")
        Integer playgroundRadiusInMeters,

        @Valid @NotNull(message = "감옥 중심 좌표를 입력해주세요.")
        CoordinatesRequest jailCenter,

        @NotNull(message = "감옥 반경을 입력해주세요.")
        @Min(value = 5, message = "감옥 반경은 최소 5m 이상이어야 합니다.")
        Integer jailRadiusInMeters
) {

}
