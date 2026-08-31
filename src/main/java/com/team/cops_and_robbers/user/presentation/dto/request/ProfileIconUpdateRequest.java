package com.team.cops_and_robbers.user.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

public record ProfileIconUpdateRequest(
        @Schema(description = "변경할 프로필 아이콘 번호 (앱 에셋 번호와 1:1 대응)", example = "2")
        @Positive(message = "프로필 아이콘 번호는 1 이상이어야 합니다.")
        int profileIcon
) {
}
