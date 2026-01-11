package com.team.cops_and_robbers.auth.application;


import com.team.cops_and_robbers.auth.application.dto.command.LoginCommand;
import com.team.cops_and_robbers.auth.application.dto.result.LoginResult;
import com.team.cops_and_robbers.auth.domain.Tokens;
import com.team.cops_and_robbers.auth.exception.AuthException;
import com.team.cops_and_robbers.auth.infrastructure.jwt.JwtTokenProvider;
import com.team.cops_and_robbers.auth.repository.RefreshTokenRepository;
import com.team.cops_and_robbers.auth.infrastructure.social.strategy.SocialLoginStrategy;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.user.domain.SocialType;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAXIMUM_NICKNAME_GENERATE_RETRY_COUNT = 10;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RandomNicknameGenerator randomNicknameGenerator;
    private final Map<SocialType, SocialLoginStrategy> socialLoginStrategies;

    /**
     * 1. 소셜 로그인 진행
     *  - 처음 로그인한 사용자의 경우 랜덤 닉네임 부여 후 DB 추가
     *  - 기 가입자는 로그인한 핸드폰의 device, fcm 정보로 DB 업데이트
     */
    @Transactional
    public LoginResult login(LoginCommand command) {
        String socialId = getSocialId(command.socialType(), command.idToken());
        AuthUserData authUserData = findOrRegisterUser(command, socialId);
        User user = authUserData.user;
        Tokens tokens = issueTokens(user);

        return new LoginResult(
                user,
                authUserData.isNewUser,
                tokens
        );
    }

    private String getSocialId(SocialType socialType, String idToken) {
        SocialLoginStrategy strategy = socialLoginStrategies.get(socialType);
        if (strategy == null) {
            throw new ApplicationException(AuthException.UNSUPPORTED_SOCIAL_TYPE);
        }
        return strategy.validateAndGetSocialId(idToken);
    }

    private AuthUserData findOrRegisterUser(LoginCommand command, String socialId) {
        return userRepository.findBySocialIdAndSocialType(socialId, command.socialType())
                .map(user -> {
                    user.updateDeviceAndFcmToken(
                            command.deviceId(), command.deviceType(), command.fcmToken()
                    );
                    return new AuthUserData(user, false);
                })

                .orElseGet(() -> {
                    String nickname = generateUniqueNickname();
                    User newUser = User.signUp(
                            socialId, command.socialType(), nickname,
                            command.deviceId(), command.deviceType(), command.fcmToken()
                    );
                    userRepository.save(newUser);

                    log.info("[SignUp] 신규 회원가입 성공 | userId={}, nickname={}, socialType={}",
                            newUser.getId(), nickname, command.socialType());
                    return new AuthUserData(newUser, true);
                });
    }

    private String generateUniqueNickname() {
        int retryCount = 0;
        String nickname;
        do {
            nickname = randomNicknameGenerator.generate();
            retryCount++;
            if (retryCount > MAXIMUM_NICKNAME_GENERATE_RETRY_COUNT) {
                log.warn("[SignUp] 닉네임 생성 재시도 횟수 초과");
                throw new ApplicationException(AuthException.NICKNAME_GENERATION_FAILED);
            }
        } while (userRepository.existsByNickname(nickname));

        return nickname;
    }

    private Tokens issueTokens(User loginUser) {
        String accessToken = jwtTokenProvider.createAccessToken(loginUser);
        String refreshToken = jwtTokenProvider.createRefreshToken(loginUser);
        refreshTokenRepository.save(loginUser.getId(), refreshToken, jwtTokenProvider.getRefreshTokenExpirationMillis());
        return new Tokens(accessToken, refreshToken);
    }


    /**
     * 2. Access token 재발급
     * - refresh token 으로 억세스 토큰을 재발급
     */
    @Transactional(readOnly = true)
    public Tokens reissueTokens(String refreshToken) {
        Long userId = jwtTokenProvider.getUserIdFromRefreshToken(refreshToken);

        String storedRefreshToken = refreshTokenRepository.findByUserId(userId);
        if (!refreshToken.equals(storedRefreshToken)) {
            throw new ApplicationException(AuthException.INVALID_TOKEN);
        }

        User user = userRepository.getByUserId(userId);
        return issueTokens(user);
    }


    private record AuthUserData(
            User user,
            boolean isNewUser
    ) {}
}
