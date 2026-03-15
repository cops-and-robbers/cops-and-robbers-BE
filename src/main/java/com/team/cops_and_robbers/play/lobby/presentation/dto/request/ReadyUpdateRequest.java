package com.team.cops_and_robbers.play.lobby.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ReadyUpdateRequest(
        @Schema(description = "준비 상태", example = "true")
        @NotNull(message = "준비 상태 값은 필수입니다.")
        Boolean isReady
) {
}
