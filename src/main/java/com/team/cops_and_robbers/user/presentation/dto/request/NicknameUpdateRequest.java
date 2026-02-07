package com.team.cops_and_robbers.user.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NicknameUpdateRequest(
        @NotBlank(message = "닉네임은 필수 입력 항목입니다.")
        @Size(max = 10, message = "닉네임은 최대 10자 입니다.")
        String nickname
) {
}
