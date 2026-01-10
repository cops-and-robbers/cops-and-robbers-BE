package com.team.cops_and_robbers.auth.presentation.dto.response;

import com.team.cops_and_robbers.auth.application.dto.result.LoginResult;

public record LoginResponse(
        Long userId,
        String nickname,
        boolean isNewUser,
        String accessToken,
        String refreshToken
) {
    public static LoginResponse from(LoginResult result) {
        return new LoginResponse(
                result.user().getId(),
                result.user().getNickname(),
                result.isNewUser(),
                result.accessToken(),
                result.refreshToken()
        );
    }
}
