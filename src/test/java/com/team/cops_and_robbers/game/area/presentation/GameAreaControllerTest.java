package com.team.cops_and_robbers.game.area.presentation;

import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.user.domain.User;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.team.cops_and_robbers.common.fixture.GameAreaFixture.CIRCLE_GAME_AREA;
import static com.team.cops_and_robbers.common.fixture.GameAreaFixture.POLYGON_GAME_AREA;
import static com.team.cops_and_robbers.common.fixture.GameFixture.FINISHED_GAME;
import static com.team.cops_and_robbers.common.fixture.GameFixture.WAITING_GAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class GameAreaControllerTest extends ControllerTest {

    private static final String GAME_AREA_API_URL = "/api/games/{gameId}/area";

    private User user;
    private String accessToken;

    @BeforeEach
    void setUp() {
        user = givenUser();
        accessToken = givenAccessToken(user);
    }

    @Nested
    @DisplayName("게임 맵 정보 조회 API")
    class GetGameArea {

        @Test
        void WAITING_상태_게임의_맵_정보_조회_성공() {
            // given
            Game game = gameRepository.save(WAITING_GAME());
            gameAreaRepository.save(CIRCLE_GAME_AREA(game));
            givenHost(game, user);

            // when
            ExtractableResponse<Response> response = authenticated(accessToken)
                    .when()
                    .get(GAME_AREA_API_URL, game.getId())
                    .then()
                    .extract();

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat(response.jsonPath().getDouble("circle.playgroundCenter.latitude")).isEqualTo(37.4979);
                softly.assertThat(response.jsonPath().getDouble("circle.playgroundCenter.longitude")).isEqualTo(127.0276);
                softly.assertThat(response.jsonPath().getInt("circle.playgroundRadiusInMeters")).isEqualTo(500);
                softly.assertThat(response.jsonPath().getInt("circle.jailRadiusInMeters")).isEqualTo(50);
            });
        }

        @Test
        void POLYGON_타입_게임의_맵_정보_조회_성공() {
            // given
            Game game = gameRepository.save(WAITING_GAME());
            gameAreaRepository.save(POLYGON_GAME_AREA(game));
            givenHost(game, user);

            // when
            ExtractableResponse<Response> response = authenticated(accessToken)
                    .when()
                    .get(GAME_AREA_API_URL, game.getId())
                    .then()
                    .extract();

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat(response.jsonPath().getString("areaType")).isEqualTo("POLYGON");
                softly.assertThat(response.jsonPath().getList("polygon.playgroundPolygon")).hasSize(4);
                softly.assertThat(response.jsonPath().getList("polygon.jailPolygon")).hasSize(4);
            });
        }

        @Test
        void 게임이_비활성_상태이면_400_BadRequest를_응답한다() {
            // given
            Game game = gameRepository.save(FINISHED_GAME());
            gameAreaRepository.save(CIRCLE_GAME_AREA(game));

            // when
            ExtractableResponse<Response> response = authenticated(accessToken)
                    .when()
                    .get(GAME_AREA_API_URL, game.getId())
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(400);
        }

        @Test
        void 해당_게임의_참가자가_아니면_404_NotFound를_응답한다() {
            // given
            Game game = gameRepository.save(WAITING_GAME());
            gameAreaRepository.save(CIRCLE_GAME_AREA(game));

            // when
            ExtractableResponse<Response> response = authenticated(accessToken)
                    .when()
                    .get(GAME_AREA_API_URL, game.getId())
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(404);
        }

        @Test
        void 인증_토큰_없이_요청하면_401_Unauthorized를_응답한다() {
            // given
            Game game = gameRepository.save(WAITING_GAME());
            gameAreaRepository.save(CIRCLE_GAME_AREA(game));
            givenHost(game, user);

            // when
            ExtractableResponse<Response> response = unauthenticated()
                    .when()
                    .get(GAME_AREA_API_URL, game.getId())
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(401);
        }

        // Deprecated: v2.13.0 강제 업데이트 적용 후 이 클래스 전체 제거
        @Nested
        @DisplayName("[하위호환] 레거시 응답 구조")
        class LegacyAreaResponse {

            @Test
            void CIRCLE_방_응답에_중첩과_평면_필드가_모두_포함된다() {
                // given
                Game game = gameRepository.save(WAITING_GAME());
                gameAreaRepository.save(CIRCLE_GAME_AREA(game));
                givenHost(game, user);

                // when
                ExtractableResponse<Response> response = authenticated(accessToken)
                        .when()
                        .get(GAME_AREA_API_URL, game.getId())
                        .then()
                        .extract();

                // then
                assertSoftly(softly -> {
                    softly.assertThat(response.statusCode()).isEqualTo(200);
                    // 중첩 구조
                    softly.assertThat(response.jsonPath().getDouble("circle.playgroundCenter.latitude")).isEqualTo(37.4979);
                    // deprecated 평면 필드
                    softly.assertThat(response.jsonPath().getDouble("playgroundCenter.latitude")).isEqualTo(37.4979);
                    softly.assertThat(response.jsonPath().getInt("playgroundRadiusInMeters")).isEqualTo(500);
                });
            }

            @Test
            void POLYGON_방_응답에는_중첩만_포함되고_평면_필드는_null이다() {
                // given
                Game game = gameRepository.save(WAITING_GAME());
                gameAreaRepository.save(POLYGON_GAME_AREA(game));
                givenHost(game, user);

                // when
                ExtractableResponse<Response> response = authenticated(accessToken)
                        .when()
                        .get(GAME_AREA_API_URL, game.getId())
                        .then()
                        .extract();

                // then
                assertSoftly(softly -> {
                    softly.assertThat(response.statusCode()).isEqualTo(200);
                    softly.assertThat(response.jsonPath().getString("areaType")).isEqualTo("POLYGON");
                    softly.assertThat(response.jsonPath().getList("polygon.playgroundPolygon")).isNotEmpty();
                    softly.assertThat((Object) response.jsonPath().get("playgroundCenter")).isNull();
                });
            }
        }
    }
}