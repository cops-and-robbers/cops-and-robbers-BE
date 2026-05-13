package com.team.cops_and_robbers.user.presentation.dto.response;

import com.team.cops_and_robbers.user.application.dto.result.GamePushAgreementResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record GamePushAgreementResponse(
        @Schema(description = "게임 푸시 알림 수신 동의 여부", example = "true")
        boolean allowGamePush
) {
    public static GamePushAgreementResponse from(GamePushAgreementResult result) {
        return new GamePushAgreementResponse(result.allowGamePush());
    }
}
