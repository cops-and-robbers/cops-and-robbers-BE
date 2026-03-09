package com.team.cops_and_robbers.user.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record DeleteAccountResponse(
        @Schema(description = "결과 메시지", example = "회원탈퇴가 완료되었습니다.")
        String message
) {
    public static DeleteAccountResponse from() {
        return new DeleteAccountResponse(
                "회원탈퇴가 완료되었습니다."
        );
    }
}
