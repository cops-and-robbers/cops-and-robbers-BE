package com.team.cops_and_robbers.auth.social.strategy;

import com.team.cops_and_robbers.user.domain.SocialType;

public interface SocialLoginStrategy {

    String validateAndGetSocialId(String socialIdToken);
    SocialType getSocialType();
}
