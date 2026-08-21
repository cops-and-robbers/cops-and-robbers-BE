package com.team.cops_and_robbers.auth.presentation;

import com.team.cops_and_robbers.auth.domain.Tokens;
import com.team.cops_and_robbers.auth.exception.AuthException;
import com.team.cops_and_robbers.auth.presentation.dto.request.AdminLoginRequest;
import com.team.cops_and_robbers.auth.presentation.dto.request.LoginRequest;
import com.team.cops_and_robbers.auth.presentation.dto.request.LogoutRequest;
import com.team.cops_and_robbers.auth.presentation.dto.request.ReissueRequest;
import com.team.cops_and_robbers.auth.presentation.dto.response.AdminLoginResponse;
import com.team.cops_and_robbers.auth.presentation.dto.response.LoginResponse;
import com.team.cops_and_robbers.auth.presentation.dto.response.ReissueResponse;
import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.common.exception.CommonException;
import com.team.cops_and_robbers.common.exception.ErrorResponse;
import com.team.cops_and_robbers.user.domain.DeviceType;
import com.team.cops_and_robbers.user.domain.Role;
import com.team.cops_and_robbers.user.domain.SocialType;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.domain.UserDevice;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

class AuthControllerTest extends ControllerTest {


    @Nested
    @DisplayName("소셜 로그인 API")
    class Login {

        @Test
        void 신규_사용자라면_회원가입을_진행하고_201_CREATED를_응답해야_한다() {
            // given
            doReturn("social_12345")
                    .when(googleLoginStrategy)
                    .validateAndGetSocialId(anyString());

            LoginRequest request = new LoginRequest(SocialType.GOOGLE, "valid_token", "fcm_token", DeviceType.IOS, "device_123");

            // when
            ExtractableResponse<Response> extract = unauthenticated()
                    .body(request)
                    .when()
                    .post("/api/auth/login")
                    .then()
                    .extract();

            // then
            LoginResponse response = extract.as(LoginResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(201);
                softly.assertThat(response.isNewUser()).isTrue();
                softly.assertThat(response.userId()).isNotNull();
                softly.assertThat(response.tokens().accessToken()).isNotNull();
                softly.assertThat(response.tokens().refreshToken()).isNotNull();
                softly.assertThat(response.requiresAgreement()).isTrue();
            });
        }

        @Test
        void 기존_사용자라면_로그인_처리_후_200_OK를_응답해야_한다() {
            // given
            UserDevice userDevice = givenUserDevice();
            User user = userDevice.getUser();
            doReturn(user.getSocialId())
                    .when(googleLoginStrategy)
                    .validateAndGetSocialId(anyString());

            LoginRequest request = new LoginRequest(SocialType.GOOGLE, "valid_token", "fcm", DeviceType.IOS, "device_456");

            // when
            ExtractableResponse<Response> extract = unauthenticated()
                    .body(request)
                    .when()
                    .post("/api/auth/login")
                    .then()
                    .extract();

            // then
            LoginResponse response = extract.as(LoginResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(response.isNewUser()).isFalse();
                softly.assertThat(response.userId()).isEqualTo(user.getId());
                softly.assertThat(response.nickname()).isEqualTo(user.getNickname());
                softly.assertThat(response.requiresAgreement()).isFalse();
            });
        }

        @Test
        void 필수_파라미터인_idToken이_누락되면_400_BAD_REQUEST를_응답해야_한다() {
            // given
            LoginRequest request = new LoginRequest(SocialType.KAKAO, null, "fcm", DeviceType.IOS, "device");

            // when
            ExtractableResponse<Response> extract = unauthenticated()
                    .body(request)
                    .when()
                    .post("/api/auth/login")
                    .then()
                    .extract();

            // then
            ErrorResponse response = extract.as(ErrorResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(400);
                softly.assertThat(response.title()).isEqualTo(CommonException.INVALID_INPUT_VALUE.getTitle());
                softly.assertThat(response.detail()).contains("idToken");
            });
        }
    }

    @Nested
    @DisplayName("회원가입 시 닉네임 언어")
    class SignUpNicknameLanguage {

        @Test
        void 일본어_요청이면_일본어_닉네임을_발급한다() {
            String nickname = signUpWith("ja-JP", "social_ja");

            assertSoftly(softly -> {
                softly.assertThat(nickname).matches("[\\p{IsHiragana}\\p{IsKatakana}ー]+\\d{4}");
            });
        }

        @Test
        void 한국어_요청이면_한국어_닉네임을_발급한다() {
            String nickname = signUpWith("ko-KR", "social_ko");

            assertSoftly(softly -> {
                softly.assertThat(nickname).matches("[가-힣]+\\d{4}");
            });
        }

        @Test
        void 영어_요청이면_영어_닉네임을_발급한다() {
            String nickname = signUpWith("en-US", "social_en");

            assertSoftly(softly -> {
                softly.assertThat(nickname).matches("[A-Za-z]+\\d{4}");
            });
        }

        @Test
        void 지원하지_않는_언어면_한국어_닉네임을_발급한다() {
            String nickname = signUpWith("fr-FR", "social_fr");

            assertSoftly(softly -> {
                softly.assertThat(nickname).matches("[가-힣]+\\d{4}");
            });
        }

        @Test
        void 헤더가_없으면_한국어_닉네임을_발급한다() {
            String nickname = signUpWith(null, "social_none");

            assertSoftly(softly -> {
                softly.assertThat(nickname).matches("[가-힣]+\\d{4}");
            });
        }

        private String signUpWith(String acceptLanguage, String socialId) {
            doReturn(socialId).when(googleLoginStrategy).validateAndGetSocialId(anyString());

            LoginRequest request = new LoginRequest(
                    SocialType.GOOGLE, "valid_token", "fcm_token", DeviceType.IOS, socialId);

            RequestSpecification spec = unauthenticated().body(request);
            if (acceptLanguage != null) {
                spec = spec.header("Accept-Language", acceptLanguage);
            }

            Long userId = spec
                    .when()
                    .post("/api/auth/login")
                    .then()
                    .extract()
                    .as(LoginResponse.class)
                    .userId();

            return userRepository.getByUserId(userId).getNickname();
        }
    }

    @Nested
    @DisplayName("토큰 재발급 API")
    class Reissue {

        @Test
        void 유효한_리프레시_토큰이면_새로운_토큰과_함께_200_OK를_응답해야_한다() {
            // given
            User user = givenUser();
            Tokens tokens = givenTokens(user);
            ReissueRequest request = new ReissueRequest(tokens.refreshToken());

            // when
            ExtractableResponse<Response> extract = unauthenticated()
                    .body(request)
                    .when()
                    .post("/api/auth/reissue")
                    .then()
                    .extract();

            // then
            ReissueResponse response = extract.as(ReissueResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(response.tokens().accessToken()).isNotNull();
                softly.assertThat(response.tokens().refreshToken()).isNotNull();
            });
        }

        @Test
        void DB에_저장된_토큰과_다른_리프레시_토큰으로_요청하면_401_UNAUTHORIZED를_응답해야_한다() {
            // given
            User user = givenUser();
            givenTokens(user);
            ReissueRequest request = new ReissueRequest("mismatched_refresh_token");

            // when
            ExtractableResponse<Response> extract = unauthenticated()
                    .body(request)
                    .when()
                    .post("/api/auth/reissue")
                    .then()
                    .extract();


            // then
            ErrorResponse response = extract.as(ErrorResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(401);
                softly.assertThat(response.title()).isEqualTo(AuthException.INVALID_TOKEN.getTitle());
            });
        }
    }

    @Nested
    @DisplayName("로그아웃 API")
    class Logout {

        @Test
        void 로그아웃_요청이_성공하면_리프레시_토큰을_삭제하고_204_NO_CONTENT를_응답해야_한다() {
            // given
            User user = givenUser();
            Tokens tokens = givenTokens(user);
            LogoutRequest request = new LogoutRequest(tokens.refreshToken());

            // when
            ExtractableResponse<Response> extract = unauthenticated()
                    .body(request)
                    .when()
                    .post("/api/auth/logout")
                    .then()
                    .extract();

            // then
            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(204);
                String storedToken = refreshTokenRepository.findByUserId(user.getId());
                softly.assertThat(storedToken).isNull();
            });
        }
    }

    @Nested
    @DisplayName("어드민 웹 로그인 API")
    class AdminLogin {

        @Test
        void ADMIN_유저라면_로그인_성공하고_200_OK를_응답해야_한다() {
            // given
            User adminUser = givenAdminUser();
            doReturn(adminUser.getSocialId())
                    .when(googleLoginStrategy)
                    .validateAndGetSocialId(anyString());

            AdminLoginRequest request = new AdminLoginRequest(SocialType.GOOGLE, "valid_token");

            // when
            ExtractableResponse<Response> extract = unauthenticated()
                    .body(request)
                    .when()
                    .post("/api/auth/admin/login")
                    .then()
                    .extract();

            // then
            AdminLoginResponse response = extract.as(AdminLoginResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(response.userId()).isEqualTo(adminUser.getId());
                softly.assertThat(response.nickname()).isEqualTo(adminUser.getNickname());
                softly.assertThat(response.role()).isEqualTo(Role.ADMIN);
                softly.assertThat(response.tokens().accessToken()).isNotNull();
                softly.assertThat(response.tokens().refreshToken()).isNotNull();
            });
        }

        @Test
        void 일반_유저라면_403_FORBIDDEN을_응답해야_한다() {
            // given
            User normalUser = givenUser();
            doReturn(normalUser.getSocialId())
                    .when(googleLoginStrategy)
                    .validateAndGetSocialId(anyString());

            AdminLoginRequest request = new AdminLoginRequest(SocialType.GOOGLE, "valid_token");

            // when
            ExtractableResponse<Response> extract = unauthenticated()
                    .body(request)
                    .when()
                    .post("/api/auth/admin/login")
                    .then()
                    .extract();

            // then
            ErrorResponse response = extract.as(ErrorResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(403);
                softly.assertThat(response.title()).isEqualTo(AuthException.FORBIDDEN_ADMIN_ONLY.getTitle());
            });
        }

        @Test
        void 미가입_유저라면_404_NOT_FOUND를_응답해야_한다() {
            // given
            doReturn("unknown_social_id")
                    .when(googleLoginStrategy)
                    .validateAndGetSocialId(anyString());

            AdminLoginRequest request = new AdminLoginRequest(SocialType.GOOGLE, "valid_token");

            // when
            ExtractableResponse<Response> extract = unauthenticated()
                    .body(request)
                    .when()
                    .post("/api/auth/admin/login")
                    .then()
                    .extract();

            // then
            ErrorResponse response = extract.as(ErrorResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(404);
                softly.assertThat(response.title()).isEqualTo(AuthException.ADMIN_USER_NOT_FOUND.getTitle());
            });
        }

        @Test
        void 필수_파라미터인_idToken이_누락되면_400_BAD_REQUEST를_응답해야_한다() {
            // given
            AdminLoginRequest request = new AdminLoginRequest(SocialType.GOOGLE, null);

            // when
            ExtractableResponse<Response> extract = unauthenticated()
                    .body(request)
                    .when()
                    .post("/api/auth/admin/login")
                    .then()
                    .extract();

            // then
            ErrorResponse response = extract.as(ErrorResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(400);
                softly.assertThat(response.title()).isEqualTo(CommonException.INVALID_INPUT_VALUE.getTitle());
                softly.assertThat(response.detail()).contains("idToken");
            });
        }
    }

    @Nested
    @DisplayName("어드민 웹 로그아웃 API")
    class AdminLogout {

        @Test
        void 로그아웃_요청이_성공하면_리프레시_토큰만_삭제하고_204_NO_CONTENT를_응답해야_한다() {
            // given
            User adminUser = givenAdminUser();
            Tokens tokens = givenTokens(adminUser);
            LogoutRequest request = new LogoutRequest(tokens.refreshToken());

            // when
            ExtractableResponse<Response> extract = unauthenticated()
                    .body(request)
                    .when()
                    .post("/api/auth/admin/logout")
                    .then()
                    .extract();

            // then
            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(204);
                String storedToken = refreshTokenRepository.findByUserId(adminUser.getId());
                softly.assertThat(storedToken).isNull();
            });
        }
    }

}
