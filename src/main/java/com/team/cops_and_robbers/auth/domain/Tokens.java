package com.team.cops_and_robbers.auth.domain;

import io.swagger.v3.oas.annotations.media.Schema;

public record Tokens(
        @Schema(description = "액세스 토큰", example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxIiwiaWF0IjoxNzY4NDk1MDA1LCJleHAiOjE3Njg0OTg2MDV9...")
        String accessToken,
        @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxIiwiaWF0IjoxNzY4NDk1MDA1LCJleHAiOjE3Njk3MDQ2MDV9...")
        String refreshToken
) {
}
