package com.team.cops_and_robbers.auth.application.dto.result;

import com.team.cops_and_robbers.auth.domain.Tokens;
import com.team.cops_and_robbers.user.domain.User;

public record LoginResult(
        User user,
        boolean isNewUser,
        Tokens tokens
) {
    public static LoginResult of(User user, boolean isNewUser, Tokens tokens) {
        return new LoginResult(user, isNewUser, tokens);
    }
}
