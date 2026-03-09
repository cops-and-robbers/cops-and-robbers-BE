package com.team.cops_and_robbers.auth.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxIiwiaWF0IjoxNzY4NDk1MDA1LCJleHAiOjE3Njk3MDQ2MDV9...")
        @NotBlank(message = "로그아웃 시 리프레시 토큰은 필수입니다.")
        String refreshToken
) {
}
