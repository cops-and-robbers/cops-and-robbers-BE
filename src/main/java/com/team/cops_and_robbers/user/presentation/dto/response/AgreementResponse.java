package com.team.cops_and_robbers.user.presentation.dto.response;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record AgreementResponse(
        @Schema(description = "이용약관 동의 여부", example = "true")
        boolean termsOfServiceAgreed,
        @Schema(description = "개인정보처리방침 동의 여부", example = "true")
        boolean privacyPolicyAgreed,
        @Schema(description = "위치정보 이용약관 동의 여부", example = "true")
        boolean locationTermsAgreed,
        @Schema(description = "필수 약관 동의 시각 (미동의 시 null)", example = "2026-04-15T10:00:00+09:00")
        String termsAgreedAt,
        @Schema(description = "마케팅 수신 동의 여부", example = "false")
        boolean marketingAgreed,
        @Schema(description = "마케팅 동의 시각 (미동의 시 null)", example = "null")
        String marketingAgreedAt
) {
    public static AgreementResponse from(User user) {
        return new AgreementResponse(
                user.isTermsOfServiceAgreed(),
                user.isPrivacyPolicyAgreed(),
                user.isLocationTermsAgreed(),
                toIsoStringOrNull(user.getTermsAgreedAt()),
                user.isAllowMarketingPush(),
                toIsoStringOrNull(user.getMarketingAgreedAt())
        );
    }

    private static String toIsoStringOrNull(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return TimestampUtil.toIsoString(localDateTime);
    }
}