package com.team.cops_and_robbers.user.presentation.dto.response;

import com.team.cops_and_robbers.user.application.dto.result.NicknameAvailability;
import io.swagger.v3.oas.annotations.media.Schema;

public record NicknameCheckResponse(
        @Schema(description = "닉네임 사용 가능 여부", example = "false")
        boolean isAvailable,
        @Schema(description = "결과 코드 (AVAILABLE, DUPLICATED_NICKNAME, BANNED_WORD_NICKNAME)", example = "DUPLICATED_NICKNAME")
        String code,
        @Schema(description = "결과 메시지", example = "이미 사용 중인 닉네임입니다.")
        String message
) {
    public static NicknameCheckResponse from(NicknameAvailability availability) {
        return switch (availability) {
            case AVAILABLE -> new NicknameCheckResponse(true, "AVAILABLE", "사용 가능한 닉네임 입니다!");
            case DUPLICATED -> new NicknameCheckResponse(false, "DUPLICATED_NICKNAME", "이미 사용 중인 닉네임입니다.");
            case BANNED_WORD -> new NicknameCheckResponse(false, "BANNED_WORD_NICKNAME", "닉네임에 부적절한 표현이 포함되어 있습니다.");
        };
    }
}
