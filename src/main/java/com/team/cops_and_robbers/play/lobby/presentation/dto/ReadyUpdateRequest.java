package com.team.cops_and_robbers.play.lobby.presentation.dto;

import jakarta.validation.constraints.NotNull;

public record ReadyUpdateRequest(
        @NotNull(message = "준비 상태 값은 필수입니다.")
        Boolean isReady
) {
}
