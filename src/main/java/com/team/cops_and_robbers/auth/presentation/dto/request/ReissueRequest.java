package com.team.cops_and_robbers.auth.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReissueRequest(
        @NotBlank(message = "억세스 토큰 재발급 시 리프레시 토큰은 필수입니다.")
        String refreshToken
) {
}
