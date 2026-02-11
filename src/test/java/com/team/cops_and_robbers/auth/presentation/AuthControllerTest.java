package com.team.cops_and_robbers.auth.presentation;

import com.team.cops_and_robbers.auth.domain.Tokens;
import com.team.cops_and_robbers.auth.exception.AuthException;
import com.team.cops_and_robbers.auth.presentation.dto.request.LoginRequest;
import com.team.cops_and_robbers.auth.presentation.dto.request.LogoutRequest;
import com.team.cops_and_robbers.auth.presentation.dto.request.ReissueRequest;
import com.team.cops_and_robbers.auth.presentation.dto.response.LoginResponse;
import com.team.cops_and_robbers.auth.presentation.dto.response.ReissueResponse;
import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.common.exception.CommonException;
import com.team.cops_and_robbers.common.exception.ErrorResponse;
import com.team.cops_and_robbers.user.domain.DeviceType;
import com.team.cops_and_robbers.user.domain.SocialType;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.domain.UserDevice;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
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

}
