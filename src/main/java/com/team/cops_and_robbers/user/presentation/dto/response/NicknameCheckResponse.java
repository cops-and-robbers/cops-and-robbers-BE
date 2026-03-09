package com.team.cops_and_robbers.user.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record NicknameCheckResponse(
        @Schema(description = "닉네임 사용 가능 여부", example = "false")
        boolean isAvailable,
        @Schema(description = "결과 메시지", example = "이미 사용 중인 닉네임입니다.")
        String message
) {
    public static NicknameCheckResponse from(boolean isDuplicated) {
        return isDuplicated ? unavailable() : available();
    }

    private static NicknameCheckResponse available() {
        return new NicknameCheckResponse(
                true,
                "사용 가능한 닉네임 입니다!"
        );
    }

    private static NicknameCheckResponse unavailable() {
        return new NicknameCheckResponse(
                false,
                "이미 사용 중인 닉네임입니다."
        );
    }
}
