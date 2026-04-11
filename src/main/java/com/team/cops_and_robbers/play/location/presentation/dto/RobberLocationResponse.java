package com.team.cops_and_robbers.play.location.presentation.dto;

import com.team.cops_and_robbers.play.location.application.dto.result.RobberLocationResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record RobberLocationResponse(
        @Schema(description = "도둑 참가자 ID", example = "1")
        Long participantId,
        @Schema(description = "도둑 닉네임", example = "거북이")
        String nickname,
        @Schema(description = "위도", example = "37.5665")
        Double latitude,
        @Schema(description = "경도", example = "126.9780")
        Double longitude
) {
    public static RobberLocationResponse from(RobberLocationResult result) {
        return new RobberLocationResponse(
                result.participantId(),
                result.nickname(),
                result.latitude(),
                result.longitude()
        );
    }
}
