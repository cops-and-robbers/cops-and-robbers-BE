package com.team.cops_and_robbers.user.presentation.dto.request;

import com.team.cops_and_robbers.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NicknameUpdateRequest(
        @Schema(description = "변경할 닉네임", example = "날렵한경찰123")
        @NotBlank(message = "닉네임은 필수 입력 항목입니다.")
        @Size(max = User.NICKNAME_MAX_LENGTH, message = "닉네임은 최대 20자 입니다.")
        String nickname
) {
}
