package com.team.cops_and_robbers.user.presentation.dto.response;

import com.team.cops_and_robbers.user.application.dto.result.CommunityPushAgreementResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record CommunityPushAgreementResponse(
        @Schema(description = "커뮤니티 푸시 알림 수신 동의 여부", example = "true")
        boolean allowCommunityPush
) {
    public static CommunityPushAgreementResponse from(CommunityPushAgreementResult result) {
        return new CommunityPushAgreementResponse(result.allowCommunityPush());
    }
}
