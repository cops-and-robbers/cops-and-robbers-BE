package com.team.cops_and_robbers.auth.infrastructure.social.strategy;

import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.team.cops_and_robbers.auth.exception.AuthException;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.exception.InfrastructureException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class FirebaseLoginStrategy implements SocialLoginStrategy {

    private final FirebaseAuth firebaseAuth;

    @Override
    public String validateAndGetSocialId(String socialIdToken) {
        try {
            FirebaseToken firebaseToken = firebaseAuth.verifyIdToken(socialIdToken);
            return firebaseToken.getUid();
        } catch (FirebaseAuthException e) {
            AuthErrorCode errorCode = e.getAuthErrorCode();
            throw switch (errorCode) {
                case EXPIRED_ID_TOKEN -> new ApplicationException(AuthException.EXPIRED_FIREBASE_TOKEN);
                case INVALID_ID_TOKEN, REVOKED_ID_TOKEN -> new ApplicationException(AuthException.INVALID_FIREBASE_TOKEN);
                default -> new InfrastructureException(AuthException.FIREBASE_SERVER_ERROR);
            };
        }
    }
}
