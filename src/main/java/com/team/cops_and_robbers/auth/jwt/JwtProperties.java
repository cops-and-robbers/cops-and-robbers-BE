package com.team.cops_and_robbers.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        TokenInfo access,
        TokenInfo refresh
) {
    public record TokenInfo(
            String secret,
            Duration expiration
    ) {
    }
}
