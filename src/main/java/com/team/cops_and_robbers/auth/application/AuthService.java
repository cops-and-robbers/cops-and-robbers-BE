package com.team.cops_and_robbers.auth.application;


import com.team.cops_and_robbers.auth.application.dto.command.LoginCommand;
import com.team.cops_and_robbers.auth.application.dto.result.LoginResult;
import com.team.cops_and_robbers.auth.exception.AuthException;
import com.team.cops_and_robbers.auth.social.strategy.SocialLoginStrategy;
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
    private final RandomNicknameGenerator randomNicknameGenerator;
    private final Map<SocialType, SocialLoginStrategy> socialLoginStrategies;

    @Transactional
    public LoginResult login(LoginCommand command) {
        String socialId = getSocialId(command.socialType(), command.idToken());
        AuthUserData authUserData = findOrRegisterUser(command, socialId);
        return new LoginResult(
                authUserData.user,
                "ACCESS_TOKEN",
                "REFRESH_TOKEN",
                authUserData.isNewUser
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

    private record AuthUserData(User user, boolean isNewUser) {}

}
