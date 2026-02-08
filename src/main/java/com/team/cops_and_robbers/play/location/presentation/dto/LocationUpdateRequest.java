package com.team.cops_and_robbers.play.location.presentation.dto;

import jakarta.validation.constraints.NotNull;

public record LocationUpdateRequest(
        @NotNull(message = "위도는 필수입니다.")
        Double latitude,

        @NotNull(message = "경도는 필수입니다.")
        Double longitude
) {
}
