package com.team.cops_and_robbers.game.game.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record CoordinatesRequest(
        @Schema(description = "위도", example = "37.5665")
        @NotNull(message = "위도를 입력해주세요.")
        @DecimalMin(value = "-90", message = "위도는 -90 이상이어야 합니다.")
        @DecimalMax(value = "90", message = "위도는 90 이하여야 합니다.")
        Double latitude,

        @Schema(description = "경도", example = "126.978")
        @NotNull(message = "경도를 입력해주세요.")
        @DecimalMin(value = "-180", message = "경도는 -180 이상이어야 합니다.")
        @DecimalMax(value = "180", message = "경도는 180 이하여야 합니다.")
        Double longitude
) {

}
