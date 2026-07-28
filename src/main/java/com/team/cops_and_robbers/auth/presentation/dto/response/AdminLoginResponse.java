package com.team.cops_and_robbers.auth.presentation.dto.response;

import com.team.cops_and_robbers.auth.application.dto.result.AdminLoginResult;
import com.team.cops_and_robbers.auth.domain.Tokens;
import com.team.cops_and_robbers.user.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;

public record AdminLoginResponse(
        @Schema(description = "유저 ID", example = "1")
        Long userId,
        @Schema(description = "닉네임", example = "민첩한괴도5308")
        String nickname,
        @Schema(description = "권한", example = "ADMIN")
        Role role,
        @Schema(description = "JWT 토큰 정보")
        Tokens tokens
) {
    public static AdminLoginResponse from(AdminLoginResult result) {
        return new AdminLoginResponse(
                result.userId(),
                result.nickname(),
                result.role(),
                result.tokens()
        );
    }
}
