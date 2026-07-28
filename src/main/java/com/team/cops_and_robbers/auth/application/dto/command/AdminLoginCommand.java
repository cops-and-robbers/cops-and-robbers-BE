package com.team.cops_and_robbers.auth.application.dto.command;

import com.team.cops_and_robbers.user.domain.SocialType;

public record AdminLoginCommand(
        SocialType socialType,
        String idToken
) {
}
