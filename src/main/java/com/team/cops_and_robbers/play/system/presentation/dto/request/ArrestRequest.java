package com.team.cops_and_robbers.play.system.presentation.dto.request;

import jakarta.validation.constraints.NotNull;

public record ArrestRequest(
        @NotNull(message = "체포할 도둑의 ID는 필수입니다.")
        Long robberParticipantId
) {
}
