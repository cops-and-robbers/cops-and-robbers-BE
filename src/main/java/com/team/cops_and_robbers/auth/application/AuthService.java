package com.team.cops_and_robbers.auth.application;


import com.team.cops_and_robbers.auth.application.dto.command.LoginCommand;
import com.team.cops_and_robbers.auth.application.dto.result.LoginResult;
import com.team.cops_and_robbers.auth.application.event.UserFcmTokenUpdatedEvent;
import com.team.cops_and_robbers.auth.domain.Tokens;
import com.team.cops_and_robbers.auth.exception.AuthException;
import com.team.cops_and_robbers.auth.infrastructure.jwt.JwtTokenProvider;
import com.team.cops_and_robbers.auth.infrastructure.social.strategy.SocialLoginStrategy;
import com.team.cops_and_robbers.auth.repository.RefreshTokenRepository;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.user.domain.SocialType;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.domain.UserDevice;
import com.team.cops_and_robbers.user.repository.UserDeviceRepository;
import com.team.cops_and_robbers.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAXIMUM_NICKNAME_GENERATE_RETRY_COUNT = 10;

    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RandomNicknameGenerator randomNicknameGenerator;
    private final Map<SocialType, SocialLoginStrategy> socialLoginStrategies;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 1. 소셜 로그인 진행
     * - 신규 사용자: 랜덤 닉네임 부여 후 DB insert
     * - 기 가입자: 로그인한 핸드폰의 device, fcm 정보로 DB 업데이트
     */
    @Transactional
    public LoginResult login(LoginCommand command) {
        String socialId = getSocialId(command.socialType(), command.idToken());
        AuthUserData authUserData = findOrRegisterUser(socialId, command.socialType());
        User user = authUserData.user;
        syncDeviceConnection(user, command);
        Tokens tokens = issueTokens(user);

        return LoginResult.of(
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

    private AuthUserData findOrRegisterUser(String socialId, SocialType socialType) {
        return userRepository.findBySocialIdAndSocialType(socialId, socialType)
                .map(user -> new AuthUserData(user, false))
                .orElseGet(() -> {
                    String nickname = generateUniqueNickname();
                    User newUser = User.signUp(socialId, socialType, nickname);
                    userRepository.save(newUser);

                    log.info("[SignUp] 신규 회원가입 성공 | userId={}, nickname={}, socialType={}",
                            newUser.getId(), nickname, socialType);
                    return new AuthUserData(newUser, true);
                });
    }

    /**
     * 2. 로그인 기기 정보 동기화
     * - 기존 기기 정보 존재 시: 토큰 및 기기 정보 업데이트
     * - 기기 정보 존재 x: 새로운 기기 정보 생성 및 연결 (UserDevice.connect)
     */
    private void syncDeviceConnection(User user, LoginCommand command) {
        Optional<UserDevice> deviceOptional = userDeviceRepository.findByUser(user);

        if (deviceOptional.isPresent()) {
            UserDevice existingDevice = deviceOptional.get();
            boolean isFcmTokenChanged = command.fcmToken() != null
                    && !command.fcmToken().equals(existingDevice.getFcmToken());
            existingDevice.reconnect(command.deviceId(), command.deviceType(), command.fcmToken());
            if (isFcmTokenChanged) {
                eventPublisher.publishEvent(new UserFcmTokenUpdatedEvent(user.getId()));
            }
        } else {
            userDeviceRepository.save(UserDevice.connect(
                    user,
                    command.deviceId(),
                    command.deviceType(),
                    command.fcmToken()
            ));
        }
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
     * 3. Access token 재발급
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

    /**
     * 4. 로그아웃
     * - 저장된 리프레시 토큰과 유저 디바이스 정보를 삭제
     * - 만료된 리프레시 토큰 전달 시에도 유저 아이디 파싱 후 디바이스 정보 삭제
     */
    @Transactional
    public void logout(String refreshToken) {
        jwtTokenProvider.getUserIdFromRefreshTokenLogout(refreshToken)
                .ifPresent(userId -> {
                    refreshTokenRepository.delete(userId);
                    userDeviceRepository.deleteByUserId(userId);
                });
    }

    private record AuthUserData(
            User user,
            boolean isNewUser
    ) {
    }
}
