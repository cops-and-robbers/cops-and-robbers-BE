package com.team.cops_and_robbers.play.system.presentation.dto.response;

import com.team.cops_and_robbers.play.system.application.dto.result.ArrestResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record ArrestResponse(
        @Schema(description = "체포된 도둑 닉네임", example = "잡힌도둑")
        String robberNickname,
        @Schema(description = "남은 도둑 수", example = "2")
        int remainingThieves
) {
    public static ArrestResponse from(ArrestResult result) {
        return new ArrestResponse(
                result.robberNickname(),
                result.remainingThieves()
        );
    }
}
