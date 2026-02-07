package com.team.cops_and_robbers.auth.infrastructure.social.strategy;

import com.google.firebase.auth.FirebaseAuth;
import com.team.cops_and_robbers.user.domain.SocialType;
import org.springframework.stereotype.Component;

@Component
public class GoogleLoginStrategy extends FirebaseLoginStrategy {

    public GoogleLoginStrategy(FirebaseAuth firebaseAuth) {
        super(firebaseAuth);
    }

    @Override
    public SocialType getSocialType() {
        return SocialType.GOOGLE;
    }
}
