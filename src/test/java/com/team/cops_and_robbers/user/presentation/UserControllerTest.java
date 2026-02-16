package com.team.cops_and_robbers.user.presentation;

import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuthException;
import com.team.cops_and_robbers.auth.domain.Tokens;
import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.exception.CommonException;
import com.team.cops_and_robbers.common.exception.ErrorResponse;
import com.team.cops_and_robbers.common.fixture.GameFixture;
import com.team.cops_and_robbers.common.fixture.GameParticipantFixture;
import com.team.cops_and_robbers.common.fixture.UserFixture;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.exception.UserException;
import com.team.cops_and_robbers.user.presentation.dto.request.NicknameUpdateRequest;
import com.team.cops_and_robbers.user.presentation.dto.response.MyPageResponse;
import com.team.cops_and_robbers.user.presentation.dto.response.NicknameCheckResponse;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

class UserControllerTest extends ControllerTest {

    @Nested
    @DisplayName("내 정보 조회 API")
    class MyPage {

        @Test
        void 유효한_토큰으로_요청하면_내_정보와_200_OK를_응답해야_한다() {
            // given
            User user = givenUser();
            String accessToken = givenAccessToken(user);

            // when
            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .when()
                    .get("/api/user/me")
                    .then()
                    .extract();

            // then
            MyPageResponse response = extract.as(MyPageResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(response.userId()).isEqualTo(user.getId());
                softly.assertThat(response.nickname()).isEqualTo(user.getNickname());
            });
        }

        @Test
        void 토큰이_없으면_401_UNAUTHORIZED를_응답해야_한다() {
            // when
            ExtractableResponse<Response> extract = unauthenticated()
                    .when()
                    .get("/api/user/me")
                    .then()
                    .extract();

            // then
            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(401);
            });
        }
    }

    @Nested
    @DisplayName("닉네임 중복 확인 API")
    class CheckDuplicate {

        @Test
        void 이미_사용중인_닉네임이라면_isAvailable이_false여야_한다() {
            // given
            User existingUser = givenUser();

            // when
            ExtractableResponse<Response> extract = unauthenticated()
                    .param("nickname", existingUser.getNickname())
                    .when()
                    .get("/api/user/check-nickname")
                    .then()
                    .extract();

            // then
            NicknameCheckResponse response = extract.as(NicknameCheckResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(response.isAvailable()).isFalse();
            });
        }

        @Test
        void 사용_가능한_닉네임이라면_isAvailable가_true여야_한다() {
            // given
            String uniqueNickname = "유니크한닉네임999";

            // when
            ExtractableResponse<Response> extract = unauthenticated()
                    .param("nickname", uniqueNickname)
                    .when()
                    .get("/api/user/check-nickname")
                    .then()
                    .extract();

            // then
            NicknameCheckResponse response = extract.as(NicknameCheckResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(response.isAvailable()).isTrue();
            });
        }

        @Test
        void 닉네임_파라미터가_누락되면_400_BAD_REQUEST를_응답해야_한다() {
            // when
            ExtractableResponse<Response> extract = unauthenticated()
                    .when()
                    .get("/api/user/check-nickname")
                    .then()
                    .extract();

            // then
            ErrorResponse response = extract.as(ErrorResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(400);
                softly.assertThat(response.detail()).contains("nickname");
            });
        }
    }

    @Nested
    @DisplayName("닉네임 변경 API")
    class UpdateNickname {

        @Test
        void 유효한_닉네임으로_변경_요청시_성공하고_204_NO_CONTENT를_응답해야_한다() {
            // given
            User user = givenUser();
            String accessToken = givenAccessToken(user);
            String newNickname = "새로운닉네임";
            NicknameUpdateRequest request = new NicknameUpdateRequest(newNickname);

            // when
            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .body(request)
                    .when()
                    .patch("/api/user/me/nickname")
                    .then()
                    .extract();

            // then
            User updatedUser = userRepository.getByUserId(user.getId());
            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(204);
                softly.assertThat(updatedUser.getNickname()).isEqualTo(newNickname);
            });
        }

        @Test
        void 이미_존재하는_닉네임으로_변경_요청시_409_CONFLICT를_응답해야_한다() {
            // given
            User me = givenUser();
            String accessToken = givenAccessToken(me);

            User otherUser = userRepository.save(UserFixture.KAKAO_USER()); // 다른 유저 생성
            NicknameUpdateRequest request = new NicknameUpdateRequest(otherUser.getNickname());

            // when
            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .body(request)
                    .when()
                    .patch("/api/user/me/nickname")
                    .then()
                    .extract();

            // then
            ErrorResponse response = extract.as(ErrorResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(409);
                softly.assertThat(response.title()).isEqualTo(UserException.DUPLICATED_NICKNAME.getTitle());
            });
        }

        @Test
        void 본인의_현재_닉네임으로_변경_요청시_변경없이_204_NO_CONTENT를_응답해야_한다() {
            // given
            User user = givenUser();
            String accessToken = givenAccessToken(user);
            NicknameUpdateRequest request = new NicknameUpdateRequest(user.getNickname());

            // when
            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .body(request)
                    .when()
                    .patch("/api/user/me/nickname")
                    .then()
                    .extract();

            // then
            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(204);
            });
        }

        @Test
        void 닉네임이_10자를_초과하면_400_BAD_REQUEST를_응답해야_한다() {
            // given
            User user = givenUser();
            String accessToken = givenAccessToken(user);
            String longNickname = "열글자가넘어가는아주긴닉네임입니다"; // 11자 이상
            NicknameUpdateRequest request = new NicknameUpdateRequest(longNickname);

            // when
            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .body(request)
                    .when()
                    .patch("/api/user/me/nickname")
                    .then()
                    .extract();

            // then
            ErrorResponse response = extract.as(ErrorResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(400);
                softly.assertThat(response.title()).isEqualTo(CommonException.INVALID_INPUT_VALUE.getTitle());
                softly.assertThat(response.detail()).contains("최대 10자");
            });
        }

        @Test
        void 닉네임이_공백이면_400_BAD_REQUEST를_응답해야_한다() {
            // given
            User user = givenUser();
            String accessToken = givenAccessToken(user);
            NicknameUpdateRequest request = new NicknameUpdateRequest(" ");

            // when
            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .body(request)
                    .when()
                    .patch("/api/user/me/nickname")
                    .then()
                    .extract();

            // then
            ErrorResponse response = extract.as(ErrorResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(400);
                softly.assertThat(response.title()).isEqualTo(CommonException.INVALID_INPUT_VALUE.getTitle());
            });
        }
    }


    @Nested
    @DisplayName("회원 탈퇴 API")
    class DeleteAccount {

        @Test
        void 회원_탈퇴_성공() {
            // given
            User user = givenUser();
            Tokens tokens = givenTokens(user);

            // when
            ExtractableResponse<Response> extract = authenticated(tokens.accessToken())
                    .when()
                    .delete("/api/user/me")
                    .then()
                    .extract();

            // then
            NicknameCheckResponse response = extract.as(NicknameCheckResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(refreshTokenRepository.findByUserId(user.getId())).isNull();
                softly.assertThatThrownBy(() ->
                        userRepository.getByUserId(user.getId())
                )
                        .isInstanceOf(ApplicationException.class)
                        .hasMessage(UserException.USER_NOT_FOUND.getDetail());
            });
        }

        @Test
        void 외부_인증_서버에_유저가_없으면_내부_유저를_삭제한다() throws Exception {
            // given
            User user = givenUser();
            Tokens tokens = givenTokens(user);
            FirebaseAuthException mockException = mock(FirebaseAuthException.class);
            when(mockException.getAuthErrorCode()).thenReturn(AuthErrorCode.USER_NOT_FOUND);
            doThrow(mockException)
                    .when(firebaseAuth).deleteUser(anyString());

            // when
            ExtractableResponse<Response> extract = authenticated(tokens.accessToken())
                    .when()
                    .delete("/api/user/me")
                    .then()
                    .extract();

            // then
            NicknameCheckResponse response = extract.as(NicknameCheckResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(refreshTokenRepository.findByUserId(user.getId())).isNull();
                softly.assertThatThrownBy(() ->
                                userRepository.getByUserId(user.getId())
                        )
                        .isInstanceOf(ApplicationException.class)
                        .hasMessage(UserException.USER_NOT_FOUND.getDetail());
            });
        }

        @Test
        void 참여중인_게임이_있다면_회원_탈퇴에_실패하고_409_CONFLICT_를_응답해야_한다() {
            // given
            User user = givenUser();
            Tokens tokens = givenTokens(user);
            givenGame(user);

            // when
            ExtractableResponse<Response> extract = authenticated(tokens.accessToken())
                    .when()
                    .delete("/api/user/me")
                    .then()
                    .extract();

            // then
            ErrorResponse response = extract.as(ErrorResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(409);
                softly.assertThat(response.title()).isEqualTo(UserException.CANNOT_WITHDRAW.getTitle());
                softly.assertThat(refreshTokenRepository.findByUserId(user.getId())).isEqualTo(tokens.refreshToken());
            });
        }

        private void givenGame(User user) {
            Game game = GameFixture.WAITING_GAME();
            GameParticipant participant = GameParticipantFixture.HOST_PARTICIPANT(game, user);
            gameRepository.save(game);
            gameParticipantRepository.save(participant);
        }
    }
}
