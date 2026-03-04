package com.team.cops_and_robbers.game.game.presentation;

import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.presentation.dto.request.AreaRequest;
import com.team.cops_and_robbers.game.game.presentation.dto.request.CoordinatesRequest;
import com.team.cops_and_robbers.game.game.presentation.dto.request.GameCreateRequest;
import com.team.cops_and_robbers.game.game.presentation.dto.request.GameSettingsRequest;
import com.team.cops_and_robbers.game.game.presentation.dto.response.GameCreateResponse;
import com.team.cops_and_robbers.user.domain.User;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.team.cops_and_robbers.common.fixture.GameFixture.FINISHED_GAME;
import static com.team.cops_and_robbers.common.fixture.GameFixture.WAITING_GAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class GameControllerTest extends ControllerTest {

    private static final String GAME_API_URL = "/api/games";
    private static final String GAME_INFO_API_URL = "/api/games/{gameId}";

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

            AreaRequest invalidArea = new AreaRequest(
                    playgroundCenter, 1000,
                    farAwayJailCenter, 100
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

    private GameCreateRequest createGameCreateRequest() {
        return new GameCreateRequest(createAreaRequest(), createSettingsRequest());
    }

    private AreaRequest createAreaRequest() {
        return new AreaRequest(
                new CoordinatesRequest(37.5665, 126.978),   // playgroundCenter
                1000,   // playgroundRadius
                new CoordinatesRequest(37.5665, 126.978),   // jailCenter
                100 // jailRadius
        );
    }

    private GameSettingsRequest createSettingsRequest() {
        return new GameSettingsRequest(30, 5, 3, 10);
    }
}