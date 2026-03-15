package com.team.cops_and_robbers.game.game.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record GameCreateRequest(
        @Schema(description = "영역 설정 정보")
        @Valid @NotNull(message = "영역 설정은 필수입니다.")
        GameAreaRequest area,

        @Schema(description = "게임 세부 설정 정보")
        @Valid @NotNull(message = "게임 세부 설정은 필수입니다.")
        GameSettingsRequest settings
) {

}
