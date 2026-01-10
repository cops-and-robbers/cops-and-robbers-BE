package com.team.cops_and_robbers.auth.presentation.dto.response;

import com.team.cops_and_robbers.auth.application.dto.result.LoginResult;
import com.team.cops_and_robbers.auth.domain.Tokens;

public record LoginResponse(
        Long userId,
        String nickname,
        Tokens tokens
) {
    public static LoginResponse from(LoginResult result) {
        return new LoginResponse(
                result.user().getId(),
                result.user().getNickname(),
                result.tokens()
        );
    }
}
