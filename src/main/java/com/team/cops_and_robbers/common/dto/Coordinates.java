package com.team.cops_and_robbers.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record Coordinates(
        @Schema(description = "위도", example = "37.5665")
        Double latitude,
        @Schema(description = "경도", example = "126.978")
        Double longitude
) {
}
