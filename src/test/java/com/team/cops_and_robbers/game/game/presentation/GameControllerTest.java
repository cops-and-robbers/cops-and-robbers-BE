package com.team.cops_and_robbers.game.game.presentation;

import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.common.fixture.GameAreaFixture;
import com.team.cops_and_robbers.common.fixture.GameFixture;
import com.team.cops_and_robbers.common.fixture.UserFixture;
import com.team.cops_and_robbers.game.area.domain.AreaType;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.presentation.dto.request.CoordinatesRequest;
import com.team.cops_and_robbers.game.game.presentation.dto.request.GameAreaRequest;
import com.team.cops_and_robbers.game.game.presentation.dto.request.GameCreateRequest;
import com.team.cops_and_robbers.game.game.presentation.dto.request.GameSettingsRequest;
import com.team.cops_and_robbers.game.game.presentation.dto.response.GameAreaUpdateResponse;
import com.team.cops_and_robbers.game.game.presentation.dto.response.GameCreateResponse;
import com.team.cops_and_robbers.game.game.presentation.dto.response.GameSettingsUpdateResponse;
import com.team.cops_and_robbers.user.domain.User;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static com.team.cops_and_robbers.common.fixture.GameFixture.FINISHED_GAME;
import static com.team.cops_and_robbers.common.fixture.GameFixture.WAITING_GAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class GameControllerTest extends ControllerTest {

    private static final String GAME_ID_PARAM = "gameId";
    private static final String GAME_API_URL = "/api/games";
    private static final String GAME_INFO_API_URL = "/api/games/{gameId}";
    private static final String AREA_URL = "/api/games/{gameId}/area";
    private static final String SETTINGS_URL = "/api/games/{gameId}/settings";

    private User host;
    private String accessToken;

    @BeforeEach
    void setUp() {
        host = givenUser();
        accessToken = givenAccessToken(host);
    }

    @Nested
    @DisplayName("게임 방 생성 API")
    class CreateGame {

        @Test
        void 게임_방_생성_성공() {
            // given
            GameCreateRequest request = createGameCreateRequest();

            // when
            ExtractableResponse<Response> response = authenticated(accessToken)
                    .body(request)
                    .when()
                    .post(GAME_API_URL)
                    .then()
                    .extract();

            // then
            GameCreateResponse result = response.as(GameCreateResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(201);
                softly.assertThat(result.gameId()).isNotNull();
                softly.assertThat(result.inviteCode()).hasSize(6);
                softly.assertThat(result.status()).isEqualTo("WAITING");
            });
        }

        @Test
        void 감옥이_플레이그라운드_범위_밖에_위치하면_400_BadRequest를_응답한다() {
            // given
            CoordinatesRequest playgroundCenter = new CoordinatesRequest(37.5665, 126.978);
            CoordinatesRequest farAwayJailCenter = new CoordinatesRequest(35.1796, 129.0756);

            GameAreaRequest invalidArea = new GameAreaRequest(
                    AreaType.CIRCLE,
                    new GameAreaRequest.CircleAreaRequest(playgroundCenter, 1000, farAwayJailCenter, 100),
                    null
            );

            GameCreateRequest request = new GameCreateRequest(invalidArea, createSettingsRequest());

            // when
            ExtractableResponse<Response> response = authenticated(accessToken)
                    .body(request)
                    .when()
                    .post(GAME_API_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(400);
        }

        @Test
        void 위치_공개_주기가_라운드_시간보다_길면_400_BadRequest를_응답한다() {
            // given (10분 라운드 < 15분 주기)
            GameSettingsRequest invalidSettings = new GameSettingsRequest(10, 15, 3, 10);
            GameCreateRequest request = new GameCreateRequest(createAreaRequest(), invalidSettings);

            // when
            ExtractableResponse<Response> response = authenticated(accessToken)
                    .body(request)
                    .when()
                    .post(GAME_API_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(400);
        }

        @Test
        void 이미_다른_활성_게임에_참여_중인_경우_409_Conflict를_응답한다() {
            // given
            authenticated(accessToken)
                    .body(createGameCreateRequest())
                    .when()
                    .post(GAME_API_URL);

            // when
            ExtractableResponse<Response> response = authenticated(accessToken)
                    .body(createGameCreateRequest())
                    .when()
                    .post(GAME_API_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(409);
        }

        @Test
        void 인증_토큰_없이_요청하면_401_Unauthorized를_응답한다() {
            // given
            GameCreateRequest request = createGameCreateRequest();

            // when
            ExtractableResponse<Response> response = unauthenticated()
                    .body(request)
                    .when()
                    .post(GAME_API_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(401);
        }

        @Test
        void 필수_약관에_미동의한_유저라면_400_BadRequest를_응답한다() {
            // given
            User unagreedUser = userRepository.save(UserFixture.USER_WITHOUT_TERMS("unagreedHost"));
            String unagreedToken = givenAccessToken(unagreedUser);

            // when
            ExtractableResponse<Response> response = authenticated(unagreedToken)
                    .body(createGameCreateRequest())
                    .when()
                    .post(GAME_API_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }
    }

    @Nested
    @DisplayName("게임 기본 설정 조회 API")
    class GetGameInfo {

        @Test
        void 게임_설정_조회_성공() {
            // given
            Game game = gameRepository.save(WAITING_GAME());
            givenHost(game, host);

            // when
            ExtractableResponse<Response> response = authenticated(accessToken)
                    .when()
                    .get(GAME_INFO_API_URL, game.getId())
                    .then()
                    .extract();

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat(response.jsonPath().getInt("roundDurationMinutes")).isEqualTo(game.getRoundDurationMinutes());
                softly.assertThat(response.jsonPath().getInt("locationRevealIntervalMinutes")).isEqualTo(game.getLocationRevealIntervalMinutes());
                softly.assertThat(response.jsonPath().getInt("policeWaitMinutes")).isEqualTo(game.getPoliceWaitMinutes());
                softly.assertThat(response.jsonPath().getInt("maxParticipants")).isEqualTo(game.getMaxParticipants());
                softly.assertThat(response.jsonPath().getBoolean("isStarted")).isFalse();
                softly.assertThat((Object) response.jsonPath().get("gameStartTime")).isNull();
            });
        }

        @Test
        void 게임이_비활성_상태이면_400_BadRequest를_응답한다() {
            // given
            Game game = gameRepository.save(FINISHED_GAME());

            // when
            ExtractableResponse<Response> response = authenticated(accessToken)
                    .when()
                    .get(GAME_INFO_API_URL, game.getId())
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(400);
        }

        @Test
        void 해당_게임의_참가자가_아니면_404_NotFound를_응답한다() {
            // given
            Game game = gameRepository.save(WAITING_GAME());

            // when
            ExtractableResponse<Response> response = authenticated(accessToken)
                    .when()
                    .get(GAME_INFO_API_URL, game.getId())
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(404);
        }

        @Test
        void 인증_토큰_없이_요청하면_401_Unauthorized를_응답한다() {
            // given
            Game game = gameRepository.save(WAITING_GAME());
            givenHost(game, host);

            // when
            ExtractableResponse<Response> response = unauthenticated()
                    .when()
                    .get(GAME_INFO_API_URL, game.getId())
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(401);
        }

        @Test
        void 존재하지_않는_게임이면_404_NotFound를_응답한다() {
            // when
            ExtractableResponse<Response> response = authenticated(accessToken)
                    .when()
                    .get(GAME_INFO_API_URL, 9999L)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(404);
        }
    }

    @Nested
    @DisplayName("게임 영역 수정 API")
    class UpdateGameArea {

        private Game waitingGame;

        @BeforeEach
        void setUp() {
            waitingGame = gameRepository.save(GameFixture.WAITING_GAME());
            gameAreaRepository.save(GameAreaFixture.CIRCLE_GAME_AREA(waitingGame));
            givenHost(waitingGame, host);
        }

        @Test
        void 영역_수정_성공() {
            // given
            GameAreaRequest request = createAreaRequest();

            // when
            ExtractableResponse<Response> response = authenticated(accessToken)
                    .pathParam(GAME_ID_PARAM, waitingGame.getId())
                    .body(request)
                    .when()
                    .put(AREA_URL)
                    .then()
                    .extract();

            // then
            GameAreaUpdateResponse result = response.as(GameAreaUpdateResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
                softly.assertThat(result.circle().playgroundRadiusInMeters()).isEqualTo(request.circle().playgroundRadiusInMeters());
                softly.assertThat(result.circle().jailRadiusInMeters()).isEqualTo(request.circle().jailRadiusInMeters());
            });
        }

        @Test
        void 감옥이_플레이그라운드_밖에_위치하면_400_BadRequest를_응답한다() {
            // given
            GameAreaRequest request = new GameAreaRequest(
                    AreaType.CIRCLE,
                    new GameAreaRequest.CircleAreaRequest(
                            new CoordinatesRequest(37.5665, 126.978),   // 서울 시청
                            1000,
                            new CoordinatesRequest(35.1796, 129.0756),  // 부산 - 명백히 밖
                            100
                    ),
                    null
            );

            // when
            ExtractableResponse<Response> response = authenticated(accessToken)
                    .pathParam(GAME_ID_PARAM, waitingGame.getId())
                    .body(request)
                    .when()
                    .put(AREA_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void 방장이_아닌_참여자가_요청하면_403_Forbidden을_응답한다() {
            // given
            User guest = givenUser("guest");
            String guestToken = givenAccessToken(guest);
            givenGuest(waitingGame, guest);

            // when
            ExtractableResponse<Response> response = authenticated(guestToken)
                    .pathParam(GAME_ID_PARAM, waitingGame.getId())
                    .body(createAreaRequest())
                    .when()
                    .put(AREA_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void 게임이_진행_중이면_400_BadRequest를_응답한다() {
            // given
            Game inProgressGame = gameRepository.save(GameFixture.IN_PROGRESS_GAME());
            gameAreaRepository.save(GameAreaFixture.CIRCLE_GAME_AREA(inProgressGame));
            User anotherHost = givenUser("anotherHost");
            String anotherHostToken = givenAccessToken(anotherHost);
            givenHost(inProgressGame, anotherHost);

            // when
            ExtractableResponse<Response> response = authenticated(anotherHostToken)
                    .pathParam(GAME_ID_PARAM, inProgressGame.getId())
                    .body(createAreaRequest())
                    .when()
                    .put(AREA_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void 인증_토큰_없이_요청하면_401_Unauthorized를_응답한다() {
            // when
            ExtractableResponse<Response> response = unauthenticated()
                    .pathParam(GAME_ID_PARAM, waitingGame.getId())
                    .body(createAreaRequest())
                    .when()
                    .put(AREA_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }

        @Test
        void 참여하지_않은_게임에_요청하면_404_NotFound를_응답한다() {
            // given
            User nonParticipant = givenUser("nonParticipant");
            String nonParticipantToken = givenAccessToken(nonParticipant);

            // when
            ExtractableResponse<Response> response = authenticated(nonParticipantToken)
                    .pathParam(GAME_ID_PARAM, waitingGame.getId())
                    .body(createAreaRequest())
                    .when()
                    .put(AREA_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }
    }

    @Nested
    @DisplayName("게임 설정 수정 API")
    class UpdateGameSettings {

        private Game waitingGame;

        @BeforeEach
        void setUp() {
            waitingGame = gameRepository.save(GameFixture.WAITING_GAME());
            gameAreaRepository.save(GameAreaFixture.CIRCLE_GAME_AREA(waitingGame));
            givenHost(waitingGame, host);
        }

        @Test
        void 설정_수정_성공() {
            // given
            GameSettingsRequest request = new GameSettingsRequest(60, 10, 5, 20);

            // when
            ExtractableResponse<Response> response = authenticated(accessToken)
                    .pathParam(GAME_ID_PARAM, waitingGame.getId())
                    .body(request)
                    .when()
                    .put(SETTINGS_URL)
                    .then()
                    .extract();

            // then
            GameSettingsUpdateResponse result = response.as(GameSettingsUpdateResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
                softly.assertThat(result.roundDurationMinutes()).isEqualTo(60);
                softly.assertThat(result.locationRevealIntervalMinutes()).isEqualTo(10);
                softly.assertThat(result.policeWaitMinutes()).isEqualTo(5);
                softly.assertThat(result.maxParticipants()).isEqualTo(20);
            });
        }

        @Test
        void 위치_공개_주기가_라운드_시간_이상이면_400_BadRequest를_응답한다() {
            // given (30분 라운드, 30분 주기 - 같아도 불가)
            GameSettingsRequest request = new GameSettingsRequest(30, 30, 5, 10);

            // when
            ExtractableResponse<Response> response = authenticated(accessToken)
                    .pathParam(GAME_ID_PARAM, waitingGame.getId())
                    .body(request)
                    .when()
                    .put(SETTINGS_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void 경찰_대기_시간이_라운드_시간_이상이면_400_BadRequest를_응답한다() {
            // given (30분 라운드, 30분 대기 - 같아도 불가)
            GameSettingsRequest request = new GameSettingsRequest(30, 5, 30, 10);

            // when
            ExtractableResponse<Response> response = authenticated(accessToken)
                    .pathParam(GAME_ID_PARAM, waitingGame.getId())
                    .body(request)
                    .when()
                    .put(SETTINGS_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void 방장이_아닌_참여자가_요청하면_403_Forbidden을_응답한다() {
            // given
            User guest = givenUser("guest");
            String guestToken = givenAccessToken(guest);
            givenGuest(waitingGame, guest);

            // when
            ExtractableResponse<Response> response = authenticated(guestToken)
                    .pathParam(GAME_ID_PARAM, waitingGame.getId())
                    .body(createSettingsRequest())
                    .when()
                    .put(SETTINGS_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void 게임이_진행_중이면_400_BadRequest를_응답한다() {
            // given
            Game inProgressGame = gameRepository.save(GameFixture.IN_PROGRESS_GAME());
            User anotherHost = givenUser("anotherHost");
            String anotherHostToken = givenAccessToken(anotherHost);
            givenHost(inProgressGame, anotherHost);

            // when
            ExtractableResponse<Response> response = authenticated(anotherHostToken)
                    .pathParam(GAME_ID_PARAM, inProgressGame.getId())
                    .body(createSettingsRequest())
                    .when()
                    .put(SETTINGS_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void 인증_토큰_없이_요청하면_401_Unauthorized를_응답한다() {
            // when
            ExtractableResponse<Response> response = unauthenticated()
                    .pathParam(GAME_ID_PARAM, waitingGame.getId())
                    .body(createSettingsRequest())
                    .when()
                    .put(SETTINGS_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }

        @Test
        void 참여하지_않은_게임에_요청하면_404_NotFound를_응답한다() {
            // given
            User nonParticipant = givenUser("nonParticipant");
            String nonParticipantToken = givenAccessToken(nonParticipant);

            // when
            ExtractableResponse<Response> response = authenticated(nonParticipantToken)
                    .pathParam(GAME_ID_PARAM, waitingGame.getId())
                    .body(createSettingsRequest())
                    .when()
                    .put(SETTINGS_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }
    }

    private GameCreateRequest createGameCreateRequest() {
        return new GameCreateRequest(createAreaRequest(), createSettingsRequest());
    }

    private GameAreaRequest createAreaRequest() {
        return new GameAreaRequest(
                AreaType.CIRCLE,
                new GameAreaRequest.CircleAreaRequest(
                        new CoordinatesRequest(37.5665, 126.978),   // playgroundCenter
                        1000,   // playgroundRadius
                        new CoordinatesRequest(37.5665, 126.978),   // jailCenter
                        100     // jailRadius
                ),
                null
        );
    }

    private GameSettingsRequest createSettingsRequest() {
        return new GameSettingsRequest(30, 5, 3, 10);
    }
}
