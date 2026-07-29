package com.team.cops_and_robbers.auth.application.dto.result;

import com.team.cops_and_robbers.auth.domain.Tokens;
import com.team.cops_and_robbers.user.domain.Role;
import com.team.cops_and_robbers.user.domain.User;

public record AdminLoginResult(
        Long userId,
        String nickname,
        Role role,
        Tokens tokens
) {
    public static AdminLoginResult of(User user, Tokens tokens) {
        return new AdminLoginResult(user.getId(), user.getNickname(), user.getRole(), tokens);
    }
}
