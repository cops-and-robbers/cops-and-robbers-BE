package com.team.cops_and_robbers.admin.application.dto.result.user;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.user.domain.Role;
import com.team.cops_and_robbers.user.domain.SocialType;
import com.team.cops_and_robbers.user.domain.User;

public record AdminUserResult(
        Long id,
        String nickname,
        SocialType socialType,
        Role role,
        Boolean termsOfServiceAgreed,
        Boolean privacyPolicyAgreed,
        Boolean locationTermsAgreed,
        String createdAt
) {
    public static AdminUserResult from(User user) {
        return new AdminUserResult(
                user.getId(),
                user.getNickname(),
                user.getSocialType(),
                user.getRole(),
                user.isTermsOfServiceAgreed(),
                user.isPrivacyPolicyAgreed(),
                user.isLocationTermsAgreed(),
                TimestampUtil.toIsoString(user.getCreatedAt())
        );
    }
}
