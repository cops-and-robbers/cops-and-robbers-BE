package com.team.cops_and_robbers.auth.social.strategy;

import com.team.cops_and_robbers.user.domain.SocialType;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class AppleLoginStrategy implements SocialLoginStrategy {

    @Override
    public String validateAndGetSocialId(String socialIdToken) {

        //== apple login logic 필요 ==//
        // -> 클라이언트에서 받은 idToken으로 소셜 로그인 리소스 서버로 부터 sub id 를 가져온다.

        ThreadLocalRandom random = ThreadLocalRandom.current(); // 임시 social id 발급용
        return "APPLE_SOCIAL_ID" + random.nextInt(100_000);
    }

    @Override
    public SocialType getSocialType() {
        return SocialType.APPLE;
    }
}
