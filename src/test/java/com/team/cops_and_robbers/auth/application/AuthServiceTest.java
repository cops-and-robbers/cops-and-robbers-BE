package com.team.cops_and_robbers.auth.application;

import com.team.cops_and_robbers.auth.application.dto.command.AdminLoginCommand;
import com.team.cops_and_robbers.auth.application.dto.command.LoginCommand;
import com.team.cops_and_robbers.auth.application.dto.result.AdminLoginResult;
import com.team.cops_and_robbers.auth.application.dto.result.LoginResult;
import com.team.cops_and_robbers.auth.application.event.UserFcmTokenUpdatedEvent;
import com.team.cops_and_robbers.auth.domain.Tokens;
import com.team.cops_and_robbers.auth.exception.AuthException;
import com.team.cops_and_robbers.auth.infrastructure.social.strategy.SocialLoginStrategy;
import com.team.cops_and_robbers.auth.repository.RefreshTokenRepository;
import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.user.domain.DeviceType;
import com.team.cops_and_robbers.user.domain.Role;
import com.team.cops_and_robbers.user.domain.SocialType;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.domain.UserDevice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

class AuthServiceTest extends ServiceUnitTest {

    @InjectMocks
    private AuthService authService;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private RandomNicknameGenerator randomNicknameGenerator;
    @Mock
    private SocialLoginStrategy socialLoginStrategy;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "socialLoginStrategies",
                Map.of(SocialType.KAKAO, socialLoginStrategy));
    }

    @Nested
    @DisplayName("소셜 로그인")
    class Login {

        @Test
        void 신규_사용자라면_회원가입을_진행하고_토큰을_발급한다() {
            // given
            LoginCommand command = createLoginCommand(SocialType.KAKAO);
            String socialId = "social_123";
            String nickname = "lucky_robber";

            when(socialLoginStrategy.validateAndGetSocialId(anyString())).thenReturn(socialId);
            when(userRepository.findBySocialIdAndSocialType(socialId, SocialType.KAKAO))
                    .thenReturn(Optional.empty());
            when(randomNicknameGenerator.generate()).thenReturn(nickname);
            when(userRepository.existsByNickname(nickname)).thenReturn(false);

            User savedUser = User.signUp(socialId, SocialType.KAKAO, nickname);
            setId(savedUser, 1L);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            when(jwtTokenProvider.createAccessToken(any())).thenReturn("access");
            when(jwtTokenProvider.createRefreshToken(any())).thenReturn("refresh");

            // when
            LoginResult result = authService.login(command);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.isNewUser()).isTrue();
                softly.assertThat(result.tokens().accessToken()).isEqualTo("access");
            });
            verify(userRepository).save(any(User.class));
            verify(userDeviceRepository).save(any(UserDevice.class));
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        void 기존_사용자라면_FCM_토큰_변경시_이벤트를_발행하고_기기_정보_업데이트_후_로그인을_진행한다() {
            // given
            LoginCommand command = new LoginCommand(SocialType.KAKAO, "token", "new_fcm_token", DeviceType.IOS, "new_device_id");
            String socialId = "social_123";
            User existingUser = User.signUp(socialId, SocialType.KAKAO, "old_nick");
            setId(existingUser, 1L);

            UserDevice existingDevice = spy(UserDevice.connect(existingUser, "old_device", DeviceType.IOS, "old_fcm"));

            when(socialLoginStrategy.validateAndGetSocialId(anyString())).thenReturn(socialId);
            when(userRepository.findBySocialIdAndSocialType(socialId, SocialType.KAKAO))
                    .thenReturn(Optional.of(existingUser));
            when(userDeviceRepository.findByUser(existingUser)).thenReturn(Optional.of(existingDevice));

            when(jwtTokenProvider.createAccessToken(any())).thenReturn("access");
            when(jwtTokenProvider.createRefreshToken(any())).thenReturn("refresh");

            // when
            LoginResult result = authService.login(command);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.isNewUser()).isFalse();
                softly.assertThat(result.user().getNickname()).isEqualTo("old_nick");
            });

            // 기존 기기 정보가 새로운 정보로 업데이트(reconnect) 되었는지 검증
            verify(existingDevice).reconnect(command.deviceId(), command.deviceType(), command.fcmToken());
            verify(eventPublisher).publishEvent(any(UserFcmTokenUpdatedEvent.class)); // 이벤트 정상 발행 검증
        }

        @Test
        void 기존_사용자라면_FCM_토큰이_동일할때_이벤트_발행없이_기기_정보만_업데이트_후_로그인을_진행한다() {
            // given
            LoginCommand command = new LoginCommand(SocialType.KAKAO, "token", "same_fcm_token", DeviceType.IOS, "new_device_id");
            String socialId = "social_123";
            User existingUser = User.signUp(socialId, SocialType.KAKAO, "old_nick");
            setId(existingUser, 1L);

            UserDevice existingDevice = spy(UserDevice.connect(existingUser, "old_device", DeviceType.IOS, "same_fcm_token"));

            when(socialLoginStrategy.validateAndGetSocialId(anyString())).thenReturn(socialId);
            when(userRepository.findBySocialIdAndSocialType(socialId, SocialType.KAKAO))
                    .thenReturn(Optional.of(existingUser));
            when(userDeviceRepository.findByUser(existingUser)).thenReturn(Optional.of(existingDevice));

            when(jwtTokenProvider.createAccessToken(any())).thenReturn("access");
            when(jwtTokenProvider.createRefreshToken(any())).thenReturn("refresh");

            // when
            authService.login(command);

            // then
            verify(existingDevice).reconnect(command.deviceId(), command.deviceType(), command.fcmToken());
            verify(eventPublisher, never()).publishEvent(any(UserFcmTokenUpdatedEvent.class));
        }
    }

    @Nested
    @DisplayName("토큰 재발급")
    class Reissue {

        @Test
        void 유효한_리프레시_토큰이면_새로운_토큰을_발급한다() {
            // given
            String refreshToken = "valid_token";
            Long userId = 1L;
            User user = User.signUp("id", SocialType.KAKAO, "nick");

            when(jwtTokenProvider.getUserIdFromRefreshToken(refreshToken)).thenReturn(userId);
            when(refreshTokenRepository.findByUserId(userId)).thenReturn(refreshToken);
            when(userRepository.getByUserId(userId)).thenReturn(user);
            when(jwtTokenProvider.createAccessToken(user)).thenReturn("new_access");
            when(jwtTokenProvider.createRefreshToken(user)).thenReturn("new_refresh");

            // when
            Tokens result = authService.reissueTokens(refreshToken);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.accessToken()).isEqualTo("new_access");
                softly.assertThat(result.refreshToken()).isEqualTo("new_refresh");
            });
            then(refreshTokenRepository).should().save(any(), any(), anyLong());
        }

        @Test
        void 저장된_리프레시_토큰과_전달받은_토큰이_일치하지_않으면_예외가_발생한다() {
            // given
            String refreshToken = "old_refresh_token";
            Long userId = 1L;

            when(jwtTokenProvider.getUserIdFromRefreshToken(refreshToken)).thenReturn(userId);
            when(refreshTokenRepository.findByUserId(userId)).thenReturn("refresh_token");

            // when & then
            assertThatThrownBy(() -> authService.reissueTokens(refreshToken))
                    .isInstanceOf(ApplicationException.class);

            then(userRepository).should(never()).getByUserId(anyLong());
        }

        @Test
        void 리프레시_토큰의_형식이_유효하지_않으면_예외가_발생한다() {
            // given
            String invalidToken = "malformed_token";
            when(jwtTokenProvider.getUserIdFromRefreshToken(invalidToken))
                    .thenThrow(new ApplicationException(AuthException.INVALID_TOKEN));

            // when & then
            assertThatThrownBy(() -> authService.reissueTokens(invalidToken))
                    .isInstanceOf(ApplicationException.class);

            then(refreshTokenRepository).should(never()).findByUserId(anyLong());
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class Logout {

        @Test
        void 유효한_토큰으로_로그아웃_시_토큰과_기기_정보를_삭제한다() {
            // given
            String validToken = "valid_refresh_token";
            Long userId = 1L;
            when(jwtTokenProvider.getUserIdFromRefreshTokenLogout(validToken)).thenReturn(Optional.of(userId));

            // when
            authService.logout(validToken);

            // then
            then(refreshTokenRepository).should().delete(userId);
            then(userDeviceRepository).should().deleteByUserId(userId);
        }

        @Test
        void 만료된_토큰이라도_유저_정보를_파싱할_수_있다면_기기_정보를_삭제한다() {
            // given
            String expiredToken = "expired_refresh_token";
            Long userId = 1L;
            when(jwtTokenProvider.getUserIdFromRefreshTokenLogout(expiredToken)).thenReturn(Optional.of(userId));

            // when
            authService.logout(expiredToken);

            // then
            then(userDeviceRepository).should().deleteByUserId(userId);
        }

        @Test
        void 유효하지_않은_토큰으로_로그아웃_시_아무_일도_일어나지_않는다() {
            // given
            String invalidToken = "invalid_token";
            when(jwtTokenProvider.getUserIdFromRefreshTokenLogout(invalidToken)).thenReturn(Optional.empty());

            // when
            authService.logout(invalidToken);

            // then
            then(refreshTokenRepository).should(never()).delete(anyLong());
            then(userDeviceRepository).should(never()).deleteByUserId(anyLong());
        }
    }

    @Nested
    @DisplayName("어드민 웹 로그인")
    class AdminLogin {

        @Test
        void ADMIN_권한_유저라면_디바이스_동기화_없이_토큰을_발급한다() {
            // given
            AdminLoginCommand command = new AdminLoginCommand(SocialType.KAKAO, "admin_token");
            String socialId = "admin_social_123";

            User adminUser = User.signUp(socialId, SocialType.KAKAO, "admin_nick");
            setId(adminUser, 1L);
            ReflectionTestUtils.setField(adminUser, "role", Role.ADMIN);

            when(socialLoginStrategy.validateAndGetSocialId(anyString())).thenReturn(socialId);
            when(userRepository.findBySocialIdAndSocialType(socialId, SocialType.KAKAO))
                    .thenReturn(Optional.of(adminUser));
            when(jwtTokenProvider.createAccessToken(any())).thenReturn("access");
            when(jwtTokenProvider.createRefreshToken(any())).thenReturn("refresh");

            // when
            AdminLoginResult result = authService.adminLogin(command);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.userId()).isEqualTo(1L);
                softly.assertThat(result.nickname()).isEqualTo("admin_nick");
                softly.assertThat(result.role()).isEqualTo(Role.ADMIN);
                softly.assertThat(result.tokens().accessToken()).isEqualTo("access");
                softly.assertThat(result.tokens().refreshToken()).isEqualTo("refresh");
            });
            then(userDeviceRepository).should(never()).findByUser(any());
            then(userDeviceRepository).should(never()).save(any());
        }

        @Test
        void ADMIN_권한이_아닌_유저라면_예외가_발생한다() {
            // given
            AdminLoginCommand command = new AdminLoginCommand(SocialType.KAKAO, "user_token");
            String socialId = "user_social_123";

            User normalUser = User.signUp(socialId, SocialType.KAKAO, "normal_nick");
            setId(normalUser, 2L);

            when(socialLoginStrategy.validateAndGetSocialId(anyString())).thenReturn(socialId);
            when(userRepository.findBySocialIdAndSocialType(socialId, SocialType.KAKAO))
                    .thenReturn(Optional.of(normalUser));

            // when & then
            assertThatThrownBy(() -> authService.adminLogin(command))
                    .isInstanceOf(ApplicationException.class);

            then(jwtTokenProvider).should(never()).createAccessToken(any());
        }

        @Test
        void 미가입_유저라면_예외가_발생한다() {
            // given
            AdminLoginCommand command = new AdminLoginCommand(SocialType.KAKAO, "unknown_token");
            String socialId = "unknown_social_123";

            when(socialLoginStrategy.validateAndGetSocialId(anyString())).thenReturn(socialId);
            when(userRepository.findBySocialIdAndSocialType(socialId, SocialType.KAKAO))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.adminLogin(command))
                    .isInstanceOf(ApplicationException.class);

            then(jwtTokenProvider).should(never()).createAccessToken(any());
        }
    }

    @Nested
    @DisplayName("어드민 웹 로그아웃")
    class AdminLogout {

        @Test
        void 유효한_토큰으로_로그아웃_시_리프레시_토큰만_삭제하고_기기_정보는_유지한다() {
            // given
            String validToken = "valid_refresh_token";
            Long userId = 1L;
            when(jwtTokenProvider.getUserIdFromRefreshTokenLogout(validToken)).thenReturn(Optional.of(userId));

            // when
            authService.adminLogout(validToken);

            // then
            then(refreshTokenRepository).should().delete(userId);
            then(userDeviceRepository).should(never()).deleteByUserId(anyLong());
        }

        @Test
        void 유효하지_않은_토큰으로_로그아웃_시_아무_일도_일어나지_않는다() {
            // given
            String invalidToken = "invalid_token";
            when(jwtTokenProvider.getUserIdFromRefreshTokenLogout(invalidToken)).thenReturn(Optional.empty());

            // when
            authService.adminLogout(invalidToken);

            // then
            then(refreshTokenRepository).should(never()).delete(anyLong());
        }
    }

    private LoginCommand createLoginCommand(SocialType type) {
        return new LoginCommand(type, "token", "fcm", DeviceType.IOS, "device123");
    }

}
