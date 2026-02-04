package com.team.cops_and_robbers.common.fixture;

import com.team.cops_and_robbers.user.domain.SocialType;
import com.team.cops_and_robbers.user.domain.User;

public class UserFixture {

    public static User USER() {
        return User.builder()
                .socialId("123456789")
                .socialType(SocialType.GOOGLE)
                .nickname("testUser")
                .allowGamePush(true)
                .allowMarketingPush(false)
                .build();
    }

    public static User USER(String nickname) {
        return User.builder()
                .socialId("123456789")
                .socialType(SocialType.KAKAO)
                .nickname(nickname)
                .allowGamePush(true)
                .allowMarketingPush(false)
                .build();
    }

    public static User KAKAO_USER() {
        return User.builder()
                .socialId("kakao_123456")
                .socialType(SocialType.KAKAO)
                .nickname("kakaoUser")
                .allowGamePush(true)
                .allowMarketingPush(false)
                .build();
    }

    public static User GOOGLE_USER() {
        return User.builder()
                .socialId("google_123456")
                .socialType(SocialType.GOOGLE)
                .nickname("googleUser")
                .allowGamePush(true)
                .allowMarketingPush(false)
                .build();
    }

    public static User APPLE_USER() {
        return User.builder()
                .socialId("apple_123456")
                .socialType(SocialType.APPLE)
                .nickname("appleUser")
                .allowGamePush(true)
                .allowMarketingPush(false)
                .build();
    }
}
