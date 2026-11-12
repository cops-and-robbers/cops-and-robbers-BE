package com.team.cops_and_robbers.game.game.presentation;

import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.common.fixture.GameAreaFixture;
import com.team.cops_and_robbers.common.fixture.GameFixture;
import com.team.cops_and_robbers.common.fixture.UserFixture;
import com.team.cops_and_robbers.game.area.domain.AreaType;
import com.team.cops_and_robbers.game.area.domain.GameArea;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.domain.GameMode;
import com.team.cops_and_robbers.game.game.presentation.dto.request.CoordinatesRequest;
import com.team.cops_and_robbers.game.game.presentation.dto.request.GameAreaRequest;
import com.team.cops_and_robbers.game.game.presentation.dto.request.GameCreateRequest;
import com.team.cops_and_robbers.game.game.presentation.dto.request.GameModeUpdateRequest;
import com.team.cops_and_robbers.game.game.presentation.dto.request.GameSettingsRequest;
import com.team.cops_and_robbers.game.game.presentation.dto.response.GameAreaUpdateResponse;
import com.team.cops_and_robbers.game.game.presentation.dto.response.GameCreateResponse;
import com.team.cops_and_robbers.game.game.presentation.dto.response.GameModeUpdateResponse;
import com.team.cops_and_robbers.game.game.presentation.dto.response.GameSettingsUpdateResponse;
import com.team.cops_and_robbers.user.domain.User;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

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
        void 게임_모드를_보내지_않으면_경찰과도둑으로_생성된다() {
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
                softly.assertThat(result.gameMode()).isEqualTo(GameMode.COPS_AND_ROBBERS);
            });
        }

        @Test
        void 게임_모드를_보내면_그_모드로_생성된다() {
            // given
            GameCreateRequest request = new GameCreateRequest(
                    createAreaRequestWithoutJail(),
                    createSettingsRequest(),
                    GameMode.TAG
            );

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
                softly.assertThat(result.gameMode()).isEqualTo(GameMode.TAG);
                softly.assertThat(gameRepository.findById(result.gameId()).orElseThrow().getGameMode())
                        .isEqualTo(GameMode.TAG);
            });
        }

        @Test
        void 술래잡기는_감옥_없이_생성된다() {
            // given
            GameCreateRequest request = new GameCreateRequest(
                    createAreaRequestWithoutJail(),
                    createSettingsRequest(),
                    GameMode.TAG
            );

            // when
            ExtractableResponse<Response> response = authenticated(accessToken)
                    .body(request)
                    .when()
                    .post(GAME_API_URL)
                    .then()
                    .extract();

            // then
            GameCreateResponse result = response.as(GameCreateResponse.class);
            GameArea gameArea = gameAreaRepository.getByGameId(result.gameId());
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(201);
                softly.assertThat(gameArea.getJailCenter()).isNull();
                softly.assertThat(gameArea.getJailRadiusInMeters()).isNull();
            });
        }

        @Test
        void 술래잡기는_POLYGON_구역도_감옥_없이_생성된다() {
            // given
            GameAreaRequest area = new GameAreaRequest(
                    AreaType.POLYGON,
                    null,
                    new GameAreaRequest.PolygonAreaRequest(createPlaygroundPolygon(), null)
            );
            GameCreateRequest request = new GameCreateRequest(area, createSettingsRequest(), GameMode.TAG);

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
                softly.assertThat(gameAreaRepository.getByGameId(result.gameId()).getJailPolygon()).isNull();
            });
        }

        @Test
        void 술래잡기에서_감옥을_보내면_400_BadRequest를_응답한다() {
            // given
            GameCreateRequest request = new GameCreateRequest(
                    createAreaRequest(),
                    createSettingsRequest(),
                    GameMode.TAG
            );

            // when
            ExtractableResponse<Response> response = authenticated(accessToken)
                    .body(request)
                    .when()
                    .post(GAME_API_URL)
                    .then()
                    .extract();

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(400);
                softly.assertThat(response.jsonPath().getString("errorCode")).isEqualTo("JAIL_AREA_NOT_ALLOWED");
            });
        }

        @Test
        void 경찰과도둑에서_감옥을_보내지_않으면_400_BadRequest를_응답한다() {
            // given
            GameCreateRequest request = new GameCreateRequest(
                    createAreaRequestWithoutJail(),
                    createSettingsRequest(),
                    GameMode.COPS_AND_ROBBERS
            );

            // when
            ExtractableResponse<Response> response = authenticated(accessToken)
                    .body(request)
                    .when()
                    .post(GAME_API_URL)
                    .then()
                    .extract();

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(400);
                softly.assertThat(response.jsonPath().getString("errorCode")).isEqualTo("JAIL_AREA_REQUIRED");
            });
        }

        @Test
        void 감옥_중심과_반경_중_하나만_보내면_400_BadRequest를_응답한다() {
            // given
            GameAreaRequest area = new GameAreaRequest(
                    AreaType.CIRCLE,
                    new GameAreaRequest.CircleAreaRequest(
                            new CoordinatesRequest(37.5665, 126.978), 1000,
                            new CoordinatesRequest(37.5665, 126.978), null
                    ),
                    null
            );
            GameCreateRequest request = new GameCreateRequest(area, createSettingsRequest(), null);

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
        void 최종_반경을_함께_보내면_응답에_그대로_내려준다() {
            // given (플레이그라운드 1000m, 감옥 100m)
            GameCreateRequest request = new GameCreateRequest(
                    createAreaRequest(),
                    new GameSettingsRequest(30, 5, 3, 10, 500),
                    null
            );

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
                softly.assertThat(result.shrinkFinalRadiusInMeters()).isEqualTo(500);
            });
        }

        @Test
        void 최종_반경이_감옥과_플레이그라운드_사이가_아니면_400_BadRequest를_응답한다() {
            // given (플레이그라운드 1000m, 감옥 100m - 경계값은 GameAreaDomainServiceTest 가 본다)
            GameCreateRequest request = new GameCreateRequest(
                    createAreaRequest(),
                    new GameSettingsRequest(30, 5, 3, 10, 1000),
                    null
            );

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
        void 감옥이_플레이그라운드_범위_밖에_위치하면_400_BadRequest를_응답한다() {
            // given
            CoordinatesRequest playgroundCenter = new CoordinatesRequest(37.5665, 126.978);
            CoordinatesRequest farAwayJailCenter = new CoordinatesRequest(35.1796, 129.0756);

            GameAreaRequest invalidArea = new GameAreaRequest(
                    AreaType.CIRCLE,
                    new GameAreaRequest.CircleAreaRequest(playgroundCenter, 1000, farAwayJailCenter, 100),
                    null
            );

            GameCreateRequest request = new GameCreateRequest(invalidArea, createSettingsRequest(), null);

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
        void POLYGON_타입에서_감옥이_플레이그라운드_범위_밖에_위치하면_400_BadRequest를_응답한다() {
            // given
            List<CoordinatesRequest> playgroundPolygon = List.of(
                    new CoordinatesRequest(37.4979, 127.0276),
                    new CoordinatesRequest(37.4979, 127.0296),
                    new CoordinatesRequest(37.4999, 127.0296),
                    new CoordinatesRequest(37.4999, 127.0276)
            );
            List<CoordinatesRequest> farAwayJailPolygon = List.of(
                    new CoordinatesRequest(35.1796, 129.0756),
                    new CoordinatesRequest(35.1796, 129.0776),
                    new CoordinatesRequest(35.1816, 129.0776),
                    new CoordinatesRequest(35.1816, 129.0756)
            );

            GameAreaRequest invalidArea = new GameAreaRequest(
                    AreaType.POLYGON,
                    null,
                    new GameAreaRequest.PolygonAreaRequest(playgroundPolygon, farAwayJailPolygon)
            );

            GameCreateRequest request = new GameCreateRequest(invalidArea, createSettingsRequest(), null);

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
        void areaType과_데이터가_불일치하면_400_BadRequest를_응답한다() {
            // given: CIRCLE 타입인데 circle 데이터 없음
            GameAreaRequest invalidArea = new GameAreaRequest(AreaType.CIRCLE, null, null);
            GameCreateRequest request = new GameCreateRequest(invalidArea, createSettingsRequest(), null);

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
            GameSettingsRequest invalidSettings = new GameSettingsRequest(10, 15, 3, 10, null);
            GameCreateRequest request = new GameCreateRequest(createAreaRequest(), invalidSettings, null);

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
        void 최대_참가_인원이_50명이면_생성된다() {
            // given
            GameSettingsRequest settings = new GameSettingsRequest(30, 5, 3, 50, null);
            GameCreateRequest request = new GameCreateRequest(createAreaRequest(), settings, null);

            // when
            ExtractableResponse<Response> response = authenticated(accessToken)
                    .body(request)
                    .when()
                    .post(GAME_API_URL)
                    .then()
                    .extract();

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(201);
                softly.assertThat(response.jsonPath().getInt("maxParticipants")).isEqualTo(50);
            });
        }

        @Test
        void 최대_참가_인원이_51명이면_400_BadRequest를_응답한다() {
            // given
            GameSettingsRequest settings = new GameSettingsRequest(30, 5, 3, 51, null);
            GameCreateRequest request = new GameCreateRequest(createAreaRequest(), settings, null);

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

        private void saveShrinkFinalRadius(int shrinkFinalRadiusInMeters) {
            authenticated(accessToken)
                    .pathParam(GAME_ID_PARAM, waitingGame.getId())
                    .body(new GameSettingsRequest(30, 5, 3, 10, shrinkFinalRadiusInMeters))
                    .when()
                    .put(SETTINGS_URL)
                    .then()
                    .statusCode(HttpStatus.OK.value());
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
        void 저장된_최종_반경보다_작은_구역으로_바꾸면_400_BadRequest를_응답한다() {
            // given (플레이그라운드 500m 구역에 최종 반경 200m 를 저장해 둔다)
            saveShrinkFinalRadius(200);

            GameAreaRequest request = new GameAreaRequest(
                    AreaType.CIRCLE,
                    new GameAreaRequest.CircleAreaRequest(
                            new CoordinatesRequest(37.4979, 127.0276),
                            150,
                            new CoordinatesRequest(37.4979, 127.0276),
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
        void 최종_반경이_저장된_상태에서_POLYGON으로_바꾸면_400_BadRequest를_응답한다() {
            // given
            saveShrinkFinalRadius(200);

            List<CoordinatesRequest> playgroundPolygon = List.of(
                    new CoordinatesRequest(37.4979, 127.0276),
                    new CoordinatesRequest(37.4979, 127.0296),
                    new CoordinatesRequest(37.4999, 127.0296),
                    new CoordinatesRequest(37.4999, 127.0276)
            );
            List<CoordinatesRequest> jailPolygon = List.of(
                    new CoordinatesRequest(37.4983, 127.0280),
                    new CoordinatesRequest(37.4983, 127.0285),
                    new CoordinatesRequest(37.4988, 127.0285),
                    new CoordinatesRequest(37.4988, 127.0280)
            );

            GameAreaRequest request = new GameAreaRequest(
                    AreaType.POLYGON,
                    null,
                    new GameAreaRequest.PolygonAreaRequest(playgroundPolygon, jailPolygon)
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
        void 저장된_최종_반경이_새_구역에도_맞으면_영역_수정에_성공한다() {
            // given
            saveShrinkFinalRadius(200);

            // when (플레이그라운드 1000m, 감옥 100m - 200m 가 그대로 유효하다)
            ExtractableResponse<Response> response = authenticated(accessToken)
                    .pathParam(GAME_ID_PARAM, waitingGame.getId())
                    .body(createAreaRequest())
                    .when()
                    .put(AREA_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
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
        void areaType과_데이터가_불일치하면_400_BadRequest를_응답한다() {
            // given: POLYGON 타입인데 polygon 데이터 없음
            GameAreaRequest request = new GameAreaRequest(AreaType.POLYGON, null, null);

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
        void POLYGON_타입에서_감옥이_플레이그라운드_밖에_위치하면_400_BadRequest를_응답한다() {
            // given
            List<CoordinatesRequest> playgroundPolygon = List.of(
                    new CoordinatesRequest(37.4979, 127.0276),
                    new CoordinatesRequest(37.4979, 127.0296),
                    new CoordinatesRequest(37.4999, 127.0296),
                    new CoordinatesRequest(37.4999, 127.0276)
            );
            List<CoordinatesRequest> farAwayJailPolygon = List.of(
                    new CoordinatesRequest(35.1796, 129.0756),
                    new CoordinatesRequest(35.1796, 129.0776),
                    new CoordinatesRequest(35.1816, 129.0776),
                    new CoordinatesRequest(35.1816, 129.0756)
            );

            GameAreaRequest request = new GameAreaRequest(
                    AreaType.POLYGON,
                    null,
                    new GameAreaRequest.PolygonAreaRequest(playgroundPolygon, farAwayJailPolygon)
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
        void 술래잡기_게임은_감옥_없이_영역을_수정한다() {
            // given
            Game tagGame = gameRepository.save(GameFixture.WAITING_TAG_GAME());
            gameAreaRepository.save(GameAreaFixture.CIRCLE_GAME_AREA_WITHOUT_JAIL(tagGame));
            User tagHost = givenUser("술래방장");
            givenHost(tagGame, tagHost);

            // when
            ExtractableResponse<Response> response = authenticated(givenAccessToken(tagHost))
                    .pathParam(GAME_ID_PARAM, tagGame.getId())
                    .body(createAreaRequestWithoutJail())
                    .when()
                    .put(AREA_URL)
                    .then()
                    .extract();

            // then
            GameAreaUpdateResponse result = response.as(GameAreaUpdateResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
                softly.assertThat(result.circle().jailCenter()).isNull();
                softly.assertThat(result.circle().jailRadiusInMeters()).isNull();
            });
        }

        @Test
        void 경찰과도둑_게임에서_감옥을_빼고_수정하면_400_BadRequest를_응답한다() {
            // when
            ExtractableResponse<Response> response = authenticated(accessToken)
                    .pathParam(GAME_ID_PARAM, waitingGame.getId())
                    .body(createAreaRequestWithoutJail())
                    .when()
                    .put(AREA_URL)
                    .then()
                    .extract();

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
                softly.assertThat(response.jsonPath().getString("errorCode")).isEqualTo("JAIL_AREA_REQUIRED");
            });
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
            GameSettingsRequest request = new GameSettingsRequest(60, 10, 5, 20, null);

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
                softly.assertThat(result.gameMode()).isEqualTo(GameMode.COPS_AND_ROBBERS);
                softly.assertThat(result.roundDurationMinutes()).isEqualTo(60);
                softly.assertThat(result.locationRevealIntervalMinutes()).isEqualTo(10);
                softly.assertThat(result.policeWaitMinutes()).isEqualTo(5);
                softly.assertThat(result.maxParticipants()).isEqualTo(20);
            });
        }

        @Test
        void 최종_반경을_함께_수정하면_응답에_그대로_내려준다() {
            // given (플레이그라운드 500m, 감옥 50m)
            GameSettingsRequest request = new GameSettingsRequest(60, 10, 5, 20, 200);

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
                softly.assertThat(result.shrinkFinalRadiusInMeters()).isEqualTo(200);
            });
        }

        @Test
        void 최종_반경을_null로_보내면_축소가_꺼진_상태로_내려온다() {
            // given
            GameSettingsRequest request = new GameSettingsRequest(60, 10, 5, 20, null);

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
                softly.assertThat(result.shrinkFinalRadiusInMeters()).isNull();
            });
        }

        @Test
        void 최종_반경이_감옥과_플레이그라운드_사이가_아니면_400_BadRequest를_응답한다() {
            // given (플레이그라운드 500m, 감옥 50m - 경계값은 GameAreaDomainServiceTest 가 본다)
            GameSettingsRequest request = new GameSettingsRequest(30, 5, 3, 10, 500);

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
        void 최종_반경이_0이면_400_BadRequest를_응답한다() {
            // given
            GameSettingsRequest request = new GameSettingsRequest(30, 5, 3, 10, 0);

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
        void POLYGON_구역이면_최종_반경을_저장할_수_없으며_400_BadRequest를_응답한다() {
            // given
            Game polygonGame = gameRepository.save(GameFixture.WAITING_GAME("POLYGN"));
            gameAreaRepository.save(GameAreaFixture.POLYGON_GAME_AREA(polygonGame));
            User polygonHost = givenUser("polygonHost");
            String polygonHostToken = givenAccessToken(polygonHost);
            givenHost(polygonGame, polygonHost);

            GameSettingsRequest request = new GameSettingsRequest(30, 5, 3, 10, 200);

            // when
            ExtractableResponse<Response> response = authenticated(polygonHostToken)
                    .pathParam(GAME_ID_PARAM, polygonGame.getId())
                    .body(request)
                    .when()
                    .put(SETTINGS_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void 위치_공개_주기가_라운드_시간_이상이면_400_BadRequest를_응답한다() {
            // given (30분 라운드, 30분 주기 - 같아도 불가)
            GameSettingsRequest request = new GameSettingsRequest(30, 30, 5, 10, null);

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
            GameSettingsRequest request = new GameSettingsRequest(30, 5, 30, 10, null);

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

    @Nested
    @DisplayName("게임 모드 변경 API")
    class UpdateGameMode {

        private static final String MODE_URL = "/api/games/{gameId}/mode";

        private Game waitingGame;

        @BeforeEach
        void setUp() {
            waitingGame = gameRepository.save(GameFixture.WAITING_GAME());
            gameAreaRepository.save(GameAreaFixture.CIRCLE_GAME_AREA(waitingGame));
            givenHost(waitingGame, host);
        }

        private ExtractableResponse<Response> requestModeChange(Long gameId, GameModeUpdateRequest request) {
            return authenticated(accessToken)
                    .pathParam(GAME_ID_PARAM, gameId)
                    .body(request)
                    .when()
                    .put(MODE_URL)
                    .then()
                    .extract();
        }

        @Test
        void 경찰과도둑에서_술래잡기로_바꾸면_감옥이_없어진다() {
            // given
            GameModeUpdateRequest request = new GameModeUpdateRequest(GameMode.TAG, createAreaRequestWithoutJail());

            // when
            ExtractableResponse<Response> response = requestModeChange(waitingGame.getId(), request);

            // then
            GameModeUpdateResponse result = response.as(GameModeUpdateResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
                softly.assertThat(result.gameMode()).isEqualTo(GameMode.TAG);
                softly.assertThat(result.settings().roundDurationMinutes()).isEqualTo(30);
                softly.assertThat(result.area().circle().jailCenter()).isNull();
                softly.assertThat(gameRepository.findById(waitingGame.getId()).orElseThrow().getGameMode())
                        .isEqualTo(GameMode.TAG);
                softly.assertThat(gameAreaRepository.getByGameId(waitingGame.getId()).getJailCenter()).isNull();
            });
        }

        @Test
        void 술래잡기에서_경찰과도둑으로_바꾸면_감옥이_생긴다() {
            // given
            Game tagGame = gameRepository.save(GameFixture.WAITING_TAG_GAME());
            gameAreaRepository.save(GameAreaFixture.CIRCLE_GAME_AREA_WITHOUT_JAIL(tagGame));
            User tagHost = givenUser("술래방장");
            givenHost(tagGame, tagHost);
            GameModeUpdateRequest request = new GameModeUpdateRequest(GameMode.COPS_AND_ROBBERS, createAreaRequest());

            // when
            ExtractableResponse<Response> response = authenticated(givenAccessToken(tagHost))
                    .pathParam(GAME_ID_PARAM, tagGame.getId())
                    .body(request)
                    .when()
                    .put(MODE_URL)
                    .then()
                    .extract();

            // then
            GameModeUpdateResponse result = response.as(GameModeUpdateResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
                softly.assertThat(result.gameMode()).isEqualTo(GameMode.COPS_AND_ROBBERS);
                softly.assertThat(result.area().circle().jailRadiusInMeters()).isEqualTo(100);
            });
        }

        @Test
        void 술래잡기로_바꾸면서_감옥을_보내면_400_BadRequest를_응답한다() {
            // given
            GameModeUpdateRequest request = new GameModeUpdateRequest(GameMode.TAG, createAreaRequest());

            // when
            ExtractableResponse<Response> response = requestModeChange(waitingGame.getId(), request);

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
                softly.assertThat(response.jsonPath().getString("errorCode")).isEqualTo("JAIL_AREA_NOT_ALLOWED");
                softly.assertThat(gameRepository.findById(waitingGame.getId()).orElseThrow().getGameMode())
                        .isEqualTo(GameMode.COPS_AND_ROBBERS);
            });
        }

        @Test
        void 새_감옥_반경이_저장된_최종_반경_이상이면_400_BadRequest를_응답한다() {
            // given (최종 반경 80m 저장 후, 감옥 100m 인 경찰과도둑으로 변경)
            Game tagGame = gameRepository.save(GameFixture.WAITING_TAG_GAME());
            tagGame.updateSettings(30, 5, 3, 10, 80);
            gameRepository.save(tagGame);
            gameAreaRepository.save(GameAreaFixture.CIRCLE_GAME_AREA_WITHOUT_JAIL(tagGame));
            User tagHost = givenUser("술래방장");
            givenHost(tagGame, tagHost);
            GameModeUpdateRequest request = new GameModeUpdateRequest(GameMode.COPS_AND_ROBBERS, createAreaRequest());

            // when
            ExtractableResponse<Response> response = authenticated(givenAccessToken(tagHost))
                    .pathParam(GAME_ID_PARAM, tagGame.getId())
                    .body(request)
                    .when()
                    .put(MODE_URL)
                    .then()
                    .extract();

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
                softly.assertThat(response.jsonPath().getString("errorCode")).isEqualTo("INVALID_SHRINK_FINAL_RADIUS");
            });
        }

        @Test
        void 이벤트_게임을_술래잡기로_바꾸면_400_BadRequest를_응답한다() {
            // given
            Game eventGame = GameFixture.WAITING_GAME();
            eventGame.updateEventGame(true);
            gameRepository.save(eventGame);
            gameAreaRepository.save(GameAreaFixture.CIRCLE_GAME_AREA(eventGame));
            User eventHost = givenUser("이벤트방장");
            givenHost(eventGame, eventHost);
            GameModeUpdateRequest request = new GameModeUpdateRequest(GameMode.TAG, createAreaRequestWithoutJail());

            // when
            ExtractableResponse<Response> response = authenticated(givenAccessToken(eventHost))
                    .pathParam(GAME_ID_PARAM, eventGame.getId())
                    .body(request)
                    .when()
                    .put(MODE_URL)
                    .then()
                    .extract();

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
                softly.assertThat(response.jsonPath().getString("errorCode")).isEqualTo("EVENT_GAME_NOT_SUPPORTED_IN_MODE");
            });
        }

        @Test
        void 게임_모드를_보내지_않으면_400_BadRequest를_응답한다() {
            // given
            GameModeUpdateRequest request = new GameModeUpdateRequest(null, createAreaRequest());

            // when
            ExtractableResponse<Response> response = requestModeChange(waitingGame.getId(), request);

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void 방장이_아닌_참여자가_요청하면_403_Forbidden을_응답한다() {
            // given
            User guest = givenUser("게스트");
            givenGuest(waitingGame, guest);
            GameModeUpdateRequest request = new GameModeUpdateRequest(GameMode.TAG, createAreaRequestWithoutJail());

            // when
            ExtractableResponse<Response> response = authenticated(givenAccessToken(guest))
                    .pathParam(GAME_ID_PARAM, waitingGame.getId())
                    .body(request)
                    .when()
                    .put(MODE_URL)
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
            User inProgressHost = givenUser("진행방장");
            givenHost(inProgressGame, inProgressHost);
            GameModeUpdateRequest request = new GameModeUpdateRequest(GameMode.TAG, createAreaRequestWithoutJail());

            // when
            ExtractableResponse<Response> response = authenticated(givenAccessToken(inProgressHost))
                    .pathParam(GAME_ID_PARAM, inProgressGame.getId())
                    .body(request)
                    .when()
                    .put(MODE_URL)
                    .then()
                    .extract();

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
                softly.assertThat(response.jsonPath().getString("errorCode")).isEqualTo("GAME_NOT_WAITING");
            });
        }
    }

    private GameCreateRequest createGameCreateRequest() {
        return new GameCreateRequest(createAreaRequest(), createSettingsRequest(), null);
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

    private GameAreaRequest createAreaRequestWithoutJail() {
        return new GameAreaRequest(
                AreaType.CIRCLE,
                new GameAreaRequest.CircleAreaRequest(
                        new CoordinatesRequest(37.5665, 126.978),   // playgroundCenter
                        1000,   // playgroundRadius
                        null,
                        null
                ),
                null
        );
    }

    private List<CoordinatesRequest> createPlaygroundPolygon() {
        return List.of(
                new CoordinatesRequest(37.4979, 127.0276),
                new CoordinatesRequest(37.4979, 127.0296),
                new CoordinatesRequest(37.4999, 127.0296),
                new CoordinatesRequest(37.4999, 127.0276)
        );
    }

    private GameSettingsRequest createSettingsRequest() {
        return new GameSettingsRequest(30, 5, 3, 10, null);
    }
}
