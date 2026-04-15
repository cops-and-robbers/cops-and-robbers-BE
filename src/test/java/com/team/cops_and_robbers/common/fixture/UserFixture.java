package com.team.cops_and_robbers.common.fixture;

import com.team.cops_and_robbers.user.domain.SocialType;
import com.team.cops_and_robbers.user.domain.User;

import java.util.UUID;

public class UserFixture {

    public static User USER() {
        return User.builder()
                .socialId("123456789")
                .socialType(SocialType.GOOGLE)
                .nickname("testUser")
                .allowGamePush(true)
                .allowMarketingPush(false)
                .termsOfServiceAgreed(true)
                .privacyPolicyAgreed(true)
                .locationTermsAgreed(true)
                .build();
    }

    public static User USER(String nickname) {
        return User.builder()
                .socialId(UUID.randomUUID().toString())
                .socialType(SocialType.KAKAO)
                .nickname(nickname)
                .allowGamePush(true)
                .allowMarketingPush(false)
                .termsOfServiceAgreed(true)
                .privacyPolicyAgreed(true)
                .locationTermsAgreed(true)
                .build();
    }

    public static User KAKAO_USER() {
        return User.builder()
                .socialId("kakao_123456")
                .socialType(SocialType.KAKAO)
                .nickname("kakaoUser")
                .allowGamePush(true)
                .allowMarketingPush(false)
                .termsOfServiceAgreed(true)
                .privacyPolicyAgreed(true)
                .locationTermsAgreed(true)
                .build();
    }

    public static User GOOGLE_USER() {
        return User.builder()
                .socialId("google_123456")
                .socialType(SocialType.GOOGLE)
                .nickname("googleUser")
                .allowGamePush(true)
                .allowMarketingPush(false)
                .termsOfServiceAgreed(true)
                .privacyPolicyAgreed(true)
                .locationTermsAgreed(true)
                .build();
    }

    public static User APPLE_USER() {
        return User.builder()
                .socialId("apple_123456")
                .socialType(SocialType.APPLE)
                .nickname("appleUser")
                .allowGamePush(true)
                .allowMarketingPush(false)
                .termsOfServiceAgreed(true)
                .privacyPolicyAgreed(true)
                .locationTermsAgreed(true)
                .build();
    }
}
