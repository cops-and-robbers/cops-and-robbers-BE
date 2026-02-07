package com.team.cops_and_robbers.user.presentation.dto.response;

public record NicknameCheckResponse(
        boolean isAvailable,
        String message
) {
    public static NicknameCheckResponse from(boolean isDuplicated) {
        return isDuplicated ? unavailable() : available();
    }

    private static NicknameCheckResponse available() {
        return new NicknameCheckResponse(
                true,
                "사용가능한 닉네임 입니다!"
        );
    }

    private static NicknameCheckResponse unavailable() {
        return new NicknameCheckResponse(
                false,
                "이미 사용 중인 닉네임입니다."
        );
    }
}
