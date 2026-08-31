package com.team.cops_and_robbers.user.application.dto.result;

import com.team.cops_and_robbers.user.domain.User;

public record CommunityPushAgreementResult(
        boolean allowCommunityPush
) {
    public static CommunityPushAgreementResult from(User user) {
        return new CommunityPushAgreementResult(user.isAllowCommunityPush());
    }
}
