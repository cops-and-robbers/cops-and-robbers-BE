package com.team.cops_and_robbers.play.distance.presentation.dto;

import jakarta.validation.constraints.NotNull;

public record DistanceUpdateRequest(
        @NotNull(message = "이동 거리는 필수입니다.")
        Double distanceMeters
) {
}
