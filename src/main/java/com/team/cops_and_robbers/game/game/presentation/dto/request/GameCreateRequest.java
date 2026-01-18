package com.team.cops_and_robbers.game.game.presentation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record GameCreateRequest(
        @Valid @NotNull(message = "영역 설정은 필수입니다.")
        AreaRequest area,

        @Valid @NotNull(message = "게임 세부 설정은 필수입니다.")
        GameSettingsRequest settings
) {

}
