package com.team.cops_and_robbers.auth.presentation.dto.response;

import com.team.cops_and_robbers.auth.application.dto.result.LoginResult;
import com.team.cops_and_robbers.auth.domain.Tokens;
import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(
        @Schema(description = "유저 ID", example = "1")
        Long userId,
        @Schema(description = "닉네임", example = "민첩한괴도5308")
        String nickname,
        @Schema(description = "JWT 토큰 정보")
        Tokens tokens,
        @Schema(description = "신규 회원 여부", example = "false")
        boolean isNewUser
) {
    public static LoginResponse from(LoginResult result) {
        return new LoginResponse(
                result.user().getId(),
                result.user().getNickname(),
                result.tokens(),
                result.isNewUser()
        );
    }
}
