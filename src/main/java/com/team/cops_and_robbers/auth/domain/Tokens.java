package com.team.cops_and_robbers.auth.domain;

public record Tokens(
        String accessToken,
        String refreshToken
) {
}
