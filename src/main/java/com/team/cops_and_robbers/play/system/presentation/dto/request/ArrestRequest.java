package com.team.cops_and_robbers.play.system.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ArrestRequest(
        @Schema(description = "체포할 도둑 참가자 ID", example = "102")
        @NotNull(message = "체포할 도둑의 ID는 필수입니다.")
        Long robberParticipantId
) {
}
