package com.team.cops_and_robbers.user.application.dto.result;

import com.team.cops_and_robbers.user.domain.User;

public record GamePushAgreementResult(
        boolean allowGamePush
) {
    public static GamePushAgreementResult from(User user) {
        return new GamePushAgreementResult(user.isAllowGamePush());
    }
}
