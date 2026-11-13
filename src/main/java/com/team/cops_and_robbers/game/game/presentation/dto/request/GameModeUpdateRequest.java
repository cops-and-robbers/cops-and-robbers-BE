package com.team.cops_and_robbers.game.game.presentation.dto.request;

import com.team.cops_and_robbers.game.game.domain.GameMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record GameModeUpdateRequest(
        @Schema(description = "바꿀 게임 모드", example = "TAG")
        @NotNull(message = "게임 모드는 필수입니다.")
        GameMode gameMode,

        @Schema(description = "새 모드에 맞는 영역 설정 정보")
        @Valid @NotNull(message = "영역 설정은 필수입니다.")
        GameAreaRequest area
) {

}
