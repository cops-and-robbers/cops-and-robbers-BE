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

import static com.team.cops_and_robbers.common.fixture.GameAreaFixture.GAME_AREA;
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
            gameAreaRepository.save(GAME_AREA(game));
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
                softly.assertThat(response.jsonPath().getDouble("playgroundCenter.latitude")).isEqualTo(37.4979);
                softly.assertThat(response.jsonPath().getDouble("playgroundCenter.longitude")).isEqualTo(127.0276);
                softly.assertThat(response.jsonPath().getInt("playgroundRadiusInMeters")).isEqualTo(500);
                softly.assertThat(response.jsonPath().getInt("jailRadiusInMeters")).isEqualTo(50);
            });
        }

        @Test
        void 게임이_비활성_상태이면_400_BadRequest를_응답한다() {
            // given
            Game game = gameRepository.save(FINISHED_GAME());
            gameAreaRepository.save(GAME_AREA(game));

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
            gameAreaRepository.save(GAME_AREA(game));

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
            gameAreaRepository.save(GAME_AREA(game));
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
    }
}