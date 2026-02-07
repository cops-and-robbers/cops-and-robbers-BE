package com.team.cops_and_robbers.auth.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @NotBlank(message = "로그아웃 시 리프레시 토큰은 필수입니다.")
        String refreshToken
) {
}
