package com.team.cops_and_robbers.auth.application.dto.result;

import com.team.cops_and_robbers.user.domain.User;

public record LoginResult(
        User user,
        String accessToken,
        String refreshToken,
        boolean isNewUser
) {
}
