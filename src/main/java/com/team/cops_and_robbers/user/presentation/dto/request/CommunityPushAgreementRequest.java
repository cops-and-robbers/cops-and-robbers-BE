package com.team.cops_and_robbers.user.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CommunityPushAgreementRequest(
        @Schema(description = "커뮤니티 푸시 알림 수신 동의 여부", example = "true")
        @NotNull(message = "커뮤니티 푸시 알림 수신 동의 여부는 필수 입력 항목입니다.")
        Boolean allowCommunityPush
) {
}
