package com.team.cops_and_robbers.user.presentation.dto.response;

import com.team.cops_and_robbers.user.domain.SocialType;
import com.team.cops_and_robbers.user.domain.User;

public record MyPageResponse(
        Long userId,
        String nickname,
        SocialType socialPlatform,
        boolean allowGamePush,
        boolean allowMarketingPush
) {
    public static MyPageResponse from(User user) {
        return new MyPageResponse(
                user.getId(),
                user.getNickname(),
                user.getSocialType(),
                user.isAllowGamePush(),
                user.isAllowMarketingPush()
        );
    }
}
