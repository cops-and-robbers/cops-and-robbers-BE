package com.team.cops_and_robbers.user.presentation.dto.response;

public record DeleteAccountResponse(
        String message
) {
    public static DeleteAccountResponse from() {
        return new DeleteAccountResponse(
                "회원탈퇴가 완료되었습니다."
        );
    }
}
