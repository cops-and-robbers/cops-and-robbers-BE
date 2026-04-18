package com.team.cops_and_robbers.history.presentation;

import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.history.domain.GameResult;
import com.team.cops_and_robbers.user.domain.User;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.team.cops_and_robbers.common.fixture.GameFixture.FINISHED_GAME;
import static com.team.cops_and_robbers.common.fixture.GameResultFixture.POLICE_WIN_RESULT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class GameResultControllerTest extends ControllerTest {

    private static final String GAME_RESULT_API_URL = "/api/game-results/{gameResultId}";

    private User user;
    private String accessToken;

    @BeforeEach
    void setUp() {
        user = givenUser();
        accessToken = givenAccessToken(user);
    }

    @Nested
    @DisplayName("게임 결과 조회 API")
    class GetGameResult {

        @Test
        void 게임_결과_조회에_성공한다() {
            // given
            Game game = gameRepository.save(FINISHED_GAME());
            givenPolice(game, user);
            GameResult gameResult = gameResultRepository.save(POLICE_WIN_RESULT(game.getId()));

            // when
            ExtractableResponse<Response> response = authenticated(accessToken)
                    .when()
                    .get(GAME_RESULT_API_URL, gameResult.getId())
                    .then()
                    .extract();

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat(response.jsonPath().getString("winnerTeam")).isEqualTo("POLICE");
                softly.assertThat(response.jsonPath().getInt("durationSeconds")).isEqualTo(300);
                softly.assertThat(response.jsonPath().getInt("arrestedRobberCount")).isEqualTo(3);
                softly.assertThat(response.jsonPath().getInt("remainingRobberCount")).isEqualTo(0);
            });
        }

        @Test
        void 해당_게임의_참가자가_아니면_404_NotFound를_응답한다() {
            // given
            Game game = gameRepository.save(FINISHED_GAME());
            GameResult gameResult = gameResultRepository.save(POLICE_WIN_RESULT(game.getId()));

            // when
            ExtractableResponse<Response> response = authenticated(accessToken)
                    .when()
                    .get(GAME_RESULT_API_URL, gameResult.getId())
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(404);
        }

        @Test
        void 게임_결과가_존재하지_않으면_404_NotFound를_응답한다() {
            // when
            ExtractableResponse<Response> response = authenticated(accessToken)
                    .when()
                    .get(GAME_RESULT_API_URL, 999L)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(404);
        }

        @Test
        void 인증_토큰_없이_요청하면_401_Unauthorized를_응답한다() {
            // given
            Game game = gameRepository.save(FINISHED_GAME());
            GameResult gameResult = gameResultRepository.save(POLICE_WIN_RESULT(game.getId()));

            // when
            ExtractableResponse<Response> response = unauthenticated()
                    .when()
                    .get(GAME_RESULT_API_URL, gameResult.getId())
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(401);
        }
    }
}
